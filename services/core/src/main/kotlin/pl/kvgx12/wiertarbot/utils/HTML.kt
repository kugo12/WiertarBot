package pl.kvgx12.wiertarbot.utils

import it.skrape.selects.DocElement
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

fun StringBuilder.appendElement(element: DocElement) = apply {
    element.element.childNodes().forEach {
        when {
            it.nodeName() == "br" -> append('\n')
            it is Element -> append(it.text())
            it is TextNode -> append(it.text())
        }
    }
}
