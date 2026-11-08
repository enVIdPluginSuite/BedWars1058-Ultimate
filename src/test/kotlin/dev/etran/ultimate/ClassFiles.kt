package dev.etran.ultimate

import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.File
import java.util.zip.ZipFile

/** Just enough of the JVM class file format to answer "what does this class reference, and what does that class offer". */
class ClassModel(
    val name: String, val access: Int, val superName: String?, val interfaces: List<String>,
    val methods: Set<String>, val fields: Set<String>
) {
    val isInterface: Boolean get() = access and 0x0200 != 0
}

data class MemberRef(val owner: String, val name: String, val desc: String, val kind: Kind) {
    enum class Kind { METHOD, INTERFACE_METHOD, FIELD }
    override fun toString(): String = "$owner.$name${if (kind == Kind.FIELD) "" else desc}"
}

class ParsedClass(val model: ClassModel, val refs: List<MemberRef>, val classRefs: Set<String>, val descriptors: Set<String>)

object ClassFiles {

    fun parse(bytes: ByteArray): ParsedClass {
        val input = DataInputStream(ByteArrayInputStream(bytes))
        input.readInt()
        input.readUnsignedShort()
        input.readUnsignedShort()
        val count = input.readUnsignedShort()

        val utf = arrayOfNulls<String>(count)
        val tag = IntArray(count)
        val a = IntArray(count)
        val b = IntArray(count)
        var i = 1
        while (i < count) {
            val t = input.readUnsignedByte()
            tag[i] = t
            when (t) {
                1 -> utf[i] = input.readUTF()
                3, 4 -> input.skipBytes(4)
                5, 6 -> {
                    input.skipBytes(8)
                    i++
                }
                7, 8, 16, 19, 20 -> a[i] = input.readUnsignedShort()
                9, 10, 11, 12, 17, 18 -> {
                    a[i] = input.readUnsignedShort()
                    b[i] = input.readUnsignedShort()
                }
                15 -> input.skipBytes(3)
                else -> error("Unknown constant pool tag $t")
            }
            i++
        }

        fun className(index: Int): String = utf[a[index]]!!

        val access = input.readUnsignedShort()
        val thisName = className(input.readUnsignedShort())
        val superIndex = input.readUnsignedShort()
        val superName = if (superIndex == 0) null else className(superIndex)
        val interfaces = List(input.readUnsignedShort()) { className(input.readUnsignedShort()) }

        fun skipAttributes() {
            repeat(input.readUnsignedShort()) {
                input.readUnsignedShort()
                var remaining = input.readInt().toLong()
                while (remaining > 0) {
                    val skipped = input.skip(remaining)
                    if (skipped <= 0) error("truncated class")
                    remaining -= skipped
                }
            }
        }

        val fields = HashSet<String>()
        val descriptors = HashSet<String>()
        repeat(input.readUnsignedShort()) {
            input.readUnsignedShort()
            fields.add(utf[input.readUnsignedShort()]!!)
            descriptors.add(utf[input.readUnsignedShort()]!!)
            skipAttributes()
        }
        val methods = HashSet<String>()
        repeat(input.readUnsignedShort()) {
            input.readUnsignedShort()
            val name = utf[input.readUnsignedShort()]!!
            val desc = utf[input.readUnsignedShort()]!!
            methods.add(name + desc)
            descriptors.add(desc)
            skipAttributes()
        }

        val refs = ArrayList<MemberRef>()
        val classRefs = HashSet<String>()
        for (idx in 1 until count) {
            when (tag[idx]) {
                7 -> classRefs.add(utf[a[idx]]!!)
                9, 10, 11 -> {
                    val owner = className(a[idx])
                    val nat = b[idx]
                    val name = utf[a[nat]]!!
                    val desc = utf[b[nat]]!!
                    val kind = when (tag[idx]) {
                        9 -> MemberRef.Kind.FIELD
                        10 -> MemberRef.Kind.METHOD
                        else -> MemberRef.Kind.INTERFACE_METHOD
                    }
                    refs.add(MemberRef(owner, name, desc, kind))
                    if (kind != MemberRef.Kind.FIELD) descriptors.add(desc)
                }
            }
        }
        return ParsedClass(ClassModel(thisName, access, superName, interfaces, methods, fields), refs, classRefs, descriptors)
    }

    fun parseDirectory(dir: File): List<ParsedClass> =
        dir.walkTopDown().filter { it.isFile && it.name.endsWith(".class") }.map { parse(it.readBytes()) }.toList()

    /** Index of every class of a jar (only the packages that matter for API checks). */
    fun indexJar(jar: File, prefixes: List<String> = listOf("org/bukkit/")): Map<String, ClassModel> {
        val result = HashMap<String, ClassModel>()
        ZipFile(jar).use { zip ->
            for (entry in zip.entries()) {
                val n = entry.name
                if (!n.endsWith(".class") || prefixes.none { n.startsWith(it) }) continue
                val model = parse(zip.getInputStream(entry).readBytes()).model
                result[model.name] = model
            }
        }
        return result
    }
}

/** Looks members up the way the JVM's linker does (class, superclasses, superinterfaces). */
class ApiIndex(val classes: Map<String, ClassModel>) {

    enum class Found { YES, NO, UNKNOWN }

    private val objectMethods = setOf("equals", "hashCode", "toString", "getClass", "notify", "notifyAll", "wait", "clone", "finalize")

    fun findMethod(owner: String, name: String, desc: String): Found {
        val model = classes[owner] ?: return if (owner.startsWith("java/")) Found.UNKNOWN else Found.NO
        if ("$name$desc" in model.methods) return Found.YES
        model.superName?.let {
            val r = findMethod(it, name, desc)
            if (r != Found.NO) return r
        }
        for (i in model.interfaces) {
            val r = findMethod(i, name, desc)
            if (r != Found.NO) return r
        }
        if (model.isInterface && name in objectMethods) return Found.UNKNOWN
        return Found.NO
    }

    fun findField(owner: String, name: String): Found {
        val model = classes[owner] ?: return if (owner.startsWith("java/")) Found.UNKNOWN else Found.NO
        if (name in model.fields) return Found.YES
        model.superName?.let {
            val r = findField(it, name)
            if (r != Found.NO) return r
        }
        for (i in model.interfaces) {
            val r = findField(i, name)
            if (r != Found.NO) return r
        }
        return Found.NO
    }

    fun staticFieldNames(cls: String): Set<String> = classes[cls]?.fields ?: emptySet()
}
