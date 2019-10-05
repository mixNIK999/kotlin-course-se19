package ru.hse.spb

import java.io.OutputStream


@DslMarker
annotation class TexMarker

fun document(init: TexDocument.() -> Unit): TexDocument {
    val doc = TexDocument()
    doc.init()
    return doc
}

class DocumentClass(private val type: String) : Element {
    override fun toOutputStream(outputStream: OutputStream, indent: String) {
        outputStream.write("\\documentclass{$type}\n".toByteArray())
    }
}

class UsePackage(private val packages: Array<out String>) : Element {
    override fun toOutputStream(outputStream: OutputStream, indent: String) {
        outputStream.write("\\usepackage{${packages.joinToString(", ")}}\n".toByteArray())
    }
}


class TexDocument : CommonEnvironment("document") {
    private val header = arrayListOf<Element>()

    fun documentclass(type: String) {
        header.add(DocumentClass(type))
    }

    fun usepackage(vararg packages: String) {
        header.add(UsePackage(packages))
    }

    fun toOutputStream(outputStream: OutputStream) {
        toOutputStream(outputStream, "")
    }

    override fun toOutputStream(outputStream: OutputStream, indent: String) {
        for (element in header) {
            element.toOutputStream(outputStream, indent)
        }
        super.toOutputStream(outputStream, indent)
    }
}


class Itemize : CommonEnvironment("itemize")
class Enumerate : ItemizeEnvironment("enumerate")
class Math : CommonEnvironment("math")
class Align : CommonEnvironment("align")
class Item(private val label: String?) : Element {
    override fun toOutputStream(outputStream: OutputStream, indent: String) {
        outputStream.write(("\\item" + if (label != null) "[$label]" else "").toByteArray())
    }
}

class Frame(frameName: String) : CommonEnvironment("frame") {
    init {
        bracesArgs.add(frameName)
    }
}

class Custom(name: String, squareArgs: Array<out Pair<String, String>>, braceArgs: List<String>) : CommonEnvironment(name) {
    init {
        squareArgs.forEach { (arg, value) -> this.squareArgs[arg] = value }
        this.bracesArgs.addAll(braceArgs)
    }
}

abstract class CommonEnvironment(name: String) : TexEnvironment(name) {
    fun frame(frameName: String, init: Frame.() -> Unit) = initElement(Frame(frameName), init)
    fun itemize(init: Itemize.() -> Unit) = initElement(Itemize(), init)
    fun enumerate(init: Enumerate.() -> Unit) = initElement(Enumerate(), init)
    fun math(init: Math.() -> Unit) = initElement(Math(), init)
    fun align(init: Align.() -> Unit) = initElement(Align(), init)
    fun custom(vararg args: Pair<String, String>, name: String, braceArgs: List<String>, init: Custom.() -> Unit) = initElement(Custom(name, args, braceArgs), init)
}

abstract class ItemizeEnvironment(name: String) : CommonEnvironment(name) {
    fun item(label: String = "") {
        children.add(Item(label))
    }
}

open class TexEnvironment(private val name: String) : Element {
    protected val children = arrayListOf<Element>()
    protected val bracesArgs = arrayListOf<String>()
    protected val squareArgs = hashMapOf<String, String>()

    override fun toOutputStream(outputStream: OutputStream, indent: String) {
        outputStream.write("$indent\\begin{$name}${showBracesArgs()}${showSquareArgs()}\n".toByteArray())
        for (c in children) {
            c.toOutputStream(outputStream, "$indent  ")
        }
        outputStream.write("$indent\\end{$name}\n".toByteArray())
    }

    private fun showSquareArgs() =
        if (bracesArgs.isNotEmpty())
            bracesArgs.joinToString("}{", prefix = "{", postfix = "}")
        else
            ""

    private fun showBracesArgs() =
        if (squareArgs.isNotEmpty()) {
            squareArgs.entries.joinToString("][", prefix = "[", postfix = "]") { "${it.key}=${it.value}" }
        } else {
            ""
        }

    protected fun <T : Element> initElement(tag: T, init: T.() -> Unit): T {
        tag.init()
        children.add(tag)
        return tag
    }

    operator fun String.unaryPlus() {
        children.add(TextElement(this))
    }
}

@TexMarker
interface Element {
    fun toOutputStream(outputStream: OutputStream, indent: String)
}

open class TextElement(private val text: String) : Element {
    override fun toOutputStream(outputStream: OutputStream, indent: String) {
        outputStream.write(
            text.trimMargin()
                .lineSequence()
                .joinToString("\n$indent", prefix = indent, postfix = "\n")
                .toByteArray()
        )
    }
}
