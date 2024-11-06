package com.sunnychung.application.multiplatform.hellohttp.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.AnnotatedString.Builder
import androidx.compose.ui.text.AnnotatedString.Range
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TtsAnnotation
import androidx.compose.ui.text.UrlAnnotation
//import androidx.compose.ui.text.getLocalAnnotations
//import androidx.compose.ui.text.getLocalParagraphStyles
//import androidx.compose.ui.text.getLocalSpanStyles
//import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap

class AnnotatedStringBuilder(capacity: Int = 16) : Appendable {

    private data class MutableRange<T>(
        val item: T,
        val start: Int,
        var end: Int = Int.MIN_VALUE,
        val tag: String = ""
    ) {
        /**
         * Create an immutable [Range] object.
         *
         * @param defaultEnd if the end is not set yet, it will be set to this value.
         */
        fun toRange(defaultEnd: Int = Int.MIN_VALUE): Range<T> {
            val end = if (end == Int.MIN_VALUE) defaultEnd else end
            check(end != Int.MIN_VALUE) { "Item.end should be set first" }
            return Range(item = item, start = start, end = end, tag = tag)
        }
    }

    private val text: StringBuilder = StringBuilder(capacity)
    private val spanStyles: MutableList<MutableRange<SpanStyle>> = mutableListOf()
    private val paragraphStyles: MutableList<MutableRange<ParagraphStyle>> = mutableListOf()
    // commented because we cannot construct an AnnotatedString with annotations. the constructor's visibility is internal. why!?
//    private val annotations: MutableList<MutableRange<out Any>> = mutableListOf()
    private val styleStack: MutableList<MutableRange<out Any>> = mutableListOf()

    /**
     * Create an [Builder] instance using the given [String].
     */
    constructor(text: String) : this() {
        append(text)
    }

    /**
     * Create an [Builder] instance using the given [AnnotatedString].
     */
    constructor(text: AnnotatedString) : this() {
        append(text)
    }

    /**
     * Returns the length of the [String].
     */
    val length: Int get() = text.length

    /**
     * Appends the given [String] to this [Builder].
     *
     * @param text the text to append
     */
    fun append(text: String) {
        this.text.append(text)
    }

    @Deprecated(
        message = "Replaced by the append(Char) method that returns an Appendable. " +
                "This method must be kept around for binary compatibility.",
        level = DeprecationLevel.HIDDEN
    )
    @Suppress("FunctionName", "unused")
    // Set the JvmName to preserve compatibility with bytecode that expects a void return type.
    @JvmName("append")
    fun deprecated_append_returning_void(char: Char) {
        append(char)
    }

    /**
     * Appends [text] to this [Builder] if non-null, and returns this [Builder].
     *
     * If [text] is an [AnnotatedString], all spans and annotations will be copied over as well.
     * No other subtypes of [CharSequence] will be treated specially. For example, any
     * platform-specific types, such as `SpannedString` on Android, will only have their text
     * copied and any other information held in the sequence, such as Android `Span`s, will be
     * dropped.
     */
    @Suppress("BuilderSetStyle")
    override fun append(text: CharSequence?): AnnotatedStringBuilder { // modified
        if (text is AnnotatedString) {
            append(text)
        } else {
            this.text.append(text)
        }
        return this
    }

    /**
     * Appends the range of [text] between [start] (inclusive) and [end] (exclusive) to this
     * [Builder] if non-null, and returns this [Builder].
     *
     * If [text] is an [AnnotatedString], all spans and annotations from [text] between
     * [start] and [end] will be copied over as well.
     * No other subtypes of [CharSequence] will be treated specially. For example, any
     * platform-specific types, such as `SpannedString` on Android, will only have their text
     * copied and any other information held in the sequence, such as Android `Span`s, will be
     * dropped.
     *
     * @param start The index of the first character in [text] to copy over (inclusive).
     * @param end The index after the last character in [text] to copy over (exclusive).
     */
    @Suppress("BuilderSetStyle")
    override fun append(text: CharSequence?, start: Int, end: Int): AnnotatedStringBuilder { // modified
        if (text is AnnotatedString) {
            append(text, start, end)
        } else {
            this.text.append(text, start, end)
        }
        return this
    }

    // Kdoc comes from interface method.
    override fun append(char: Char): AnnotatedStringBuilder { // modified
        this.text.append(char)
        return this
    }

    /**
     * Appends the given [AnnotatedString] to this [Builder].
     *
     * @param text the text to append
     */
    fun append(text: AnnotatedString) {
        val start = this.text.length
        this.text.append(text.text)
        // offset every style with start and add to the builder
        text.spanStyles.forEach { // modified
            addStyle(it.item, start + it.start, start + it.end, it.tag) // modified
        }
        text.paragraphStyles.forEach { // modified
            addStyle(it.item, start + it.start, start + it.end, it.tag) // modified
        }

        // modified because of no access to annotations
//        text.annotations.forEach {
//            annotations.add(
//                MutableRange(it.item, start + it.start, start + it.end, it.tag)
//            )
//        }
    }

    /**
     * Appends the range of [text] between [start] (inclusive) and [end] (exclusive) to this
     * [Builder]. All spans and annotations from [text] between [start] and [end] will be copied
     * over as well.
     *
     * @param start The index of the first character in [text] to copy over (inclusive).
     * @param end The index after the last character in [text] to copy over (exclusive).
     */
    @Suppress("BuilderSetStyle")
//    fun append(text: AnnotatedString, start: Int, end: Int) {
//        val insertionStart = this.text.length
//        this.text.append(text.text, start, end)
//        // offset every style with insertionStart and add to the builder
//        text.getLocalSpanStyles(start, end)?.fastForEach {
//            addStyle(it.item, insertionStart + it.start, insertionStart + it.end)
//        }
//        text.getLocalParagraphStyles(start, end)?.fastForEach {
//            addStyle(it.item, insertionStart + it.start, insertionStart + it.end)
//        }
//
//        text.getLocalAnnotations(start, end)?.fastForEach {
//            annotations.add(
//                MutableRange(
//                    it.item,
//                    insertionStart + it.start,
//                    insertionStart + it.end,
//                    it.tag
//                )
//            )
//        }
//    }

    /**
     * Set a [SpanStyle] for the given [range].
     *
     * @param style [SpanStyle] to be applied
     * @param start the inclusive starting offset of the range
     * @param end the exclusive end offset of the range
     */
    fun addStyle(style: SpanStyle, start: Int, end: Int, tag: String) { // modified
        spanStyles.add(MutableRange(item = style, start = start, end = end, tag = tag)) // modified
    }

    /**
     * Set a [ParagraphStyle] for the given [range]. When a [ParagraphStyle] is applied to the
     * [AnnotatedString], it will be rendered as a separate paragraph.
     *
     * @param style [ParagraphStyle] to be applied
     * @param start the inclusive starting offset of the range
     * @param end the exclusive end offset of the range
     */
    fun addStyle(style: ParagraphStyle, start: Int, end: Int, tag: String) { // modified
        paragraphStyles.add(MutableRange(item = style, start = start, end = end, tag = tag)) // modified
    }

    /**
     * Set an Annotation for the given [range].
     *
     * @param tag the tag used to distinguish annotations
     * @param annotation the string annotation that is attached
     * @param start the inclusive starting offset of the range
     * @param end the exclusive end offset of the range
     * @see getStringAnnotations
     * @sample androidx.compose.ui.text.samples.AnnotatedStringAddStringAnnotationSample
     */
//    fun addStringAnnotation(tag: String, annotation: String, start: Int, end: Int) {
//        annotations.add(MutableRange(annotation, start, end, tag))
//    }

    /**
     * Set a [TtsAnnotation] for the given [range].
     *
     * @param ttsAnnotation an object that stores text to speech metadata that intended for the
     * TTS engine.
     * @param start the inclusive starting offset of the range
     * @param end the exclusive end offset of the range
     * @see getStringAnnotations
     * @sample androidx.compose.ui.text.samples.AnnotatedStringAddStringAnnotationSample
     */
//    @ExperimentalTextApi
//    @Suppress("SetterReturnsThis")
//    fun addTtsAnnotation(ttsAnnotation: TtsAnnotation, start: Int, end: Int) {
//        annotations.add(MutableRange(ttsAnnotation, start, end))
//    }

    /**
     * Set a [UrlAnnotation] for the given [range]. URLs may be treated specially by screen
     * readers, including being identified while reading text with an audio icon or being
     * summarized in a links menu.
     *
     * @param urlAnnotation A [UrlAnnotation] object that stores the URL being linked to.
     * @param start the inclusive starting offset of the range
     * @param end the exclusive end offset of the range
     * @see getStringAnnotations
     * @sample androidx.compose.ui.text.samples.AnnotatedStringAddStringAnnotationSample
     */
//    @ExperimentalTextApi
//    @Suppress("SetterReturnsThis")
//    fun addUrlAnnotation(urlAnnotation: UrlAnnotation, start: Int, end: Int) {
//        annotations.add(MutableRange(urlAnnotation, start, end))
//    }

    /**
     * Applies the given [SpanStyle] to any appended text until a corresponding [pop] is
     * called.
     *
     * @sample androidx.compose.ui.text.samples.AnnotatedStringBuilderPushSample
     *
     * @param style SpanStyle to be applied
     */
    fun pushStyle(style: SpanStyle): Int {
        MutableRange(item = style, start = text.length).also {
            styleStack.add(it)
            spanStyles.add(it)
        }
        return styleStack.size - 1
    }

    /**
     * Applies the given [ParagraphStyle] to any appended text until a corresponding [pop]
     * is called.
     *
     * @sample androidx.compose.ui.text.samples.AnnotatedStringBuilderPushParagraphStyleSample
     *
     * @param style ParagraphStyle to be applied
     */
    fun pushStyle(style: ParagraphStyle): Int {
        MutableRange(item = style, start = text.length).also {
            styleStack.add(it)
            paragraphStyles.add(it)
        }
        return styleStack.size - 1
    }

    /**
     * Attach the given [annotation] to any appended text until a corresponding [pop]
     * is called.
     *
     * @sample androidx.compose.ui.text.samples.AnnotatedStringBuilderPushStringAnnotationSample
     *
     * @param tag the tag used to distinguish annotations
     * @param annotation the string annotation attached on this AnnotatedString
     * @see getStringAnnotations
     * @see Range
     */
//    fun pushStringAnnotation(tag: String, annotation: String): Int {
//        MutableRange(item = annotation, start = text.length, tag = tag).also {
//            styleStack.add(it)
//            annotations.add(it)
//        }
//        return styleStack.size - 1
//    }

    /**
     * Attach the given [ttsAnnotation] to any appended text until a corresponding [pop]
     * is called.
     *
     * @sample androidx.compose.ui.text.samples.AnnotatedStringBuilderPushStringAnnotationSample
     *
     * @param ttsAnnotation an object that stores text to speech metadata that intended for the
     * TTS engine.
     * @see getStringAnnotations
     * @see Range
     */
//    fun pushTtsAnnotation(ttsAnnotation: TtsAnnotation): Int {
//        MutableRange(item = ttsAnnotation, start = text.length).also {
//            styleStack.add(it)
//            annotations.add(it)
//        }
//        return styleStack.size - 1
//    }

    /**
     * Attach the given [UrlAnnotation] to any appended text until a corresponding [pop]
     * is called.
     *
     * @sample androidx.compose.ui.text.samples.AnnotatedStringBuilderPushStringAnnotationSample
     *
     * @param urlAnnotation A [UrlAnnotation] object that stores the URL being linked to.
     * @see getStringAnnotations
     * @see Range
     */
//    @Suppress("BuilderSetStyle")
//    @ExperimentalTextApi
//    fun pushUrlAnnotation(urlAnnotation: UrlAnnotation): Int {
//        MutableRange(item = urlAnnotation, start = text.length).also {
//            styleStack.add(it)
//            annotations.add(it)
//        }
//        return styleStack.size - 1
//    }

    /**
     * Ends the style or annotation that was added via a push operation before.
     *
     * @see pushStyle
     * @see pushStringAnnotation
     */
    fun pop() {
        check(styleStack.isNotEmpty()) { "Nothing to pop." }
        // pop the last element
        val item = styleStack.removeAt(styleStack.size - 1)
        item.end = text.length
    }

    /**
     * Ends the styles or annotation up to and `including` the [pushStyle] or
     * [pushStringAnnotation] that returned the given index.
     *
     * @param index the result of the a previous [pushStyle] or [pushStringAnnotation] in order
     * to pop to
     *
     * @see pop
     * @see pushStyle
     * @see pushStringAnnotation
     */
    fun pop(index: Int) {
        check(index < styleStack.size) { "$index should be less than ${styleStack.size}" }
        while ((styleStack.size - 1) >= index) {
            pop()
        }
    }

    /**
     * Constructs an [AnnotatedString] based on the configurations applied to the [Builder].
     */
    fun toAnnotatedString(): AnnotatedString {
        // modified
        return AnnotatedString(
            text = text.toString(),
            spanStyles = spanStyles
                .fastMap { it.toRange(text.length) }
                ,
            paragraphStyles = paragraphStyles
                .fastMap { it.toRange(text.length) }
                ,
//            annotations = annotations
//                .fastMap { it.toRange(text.length) }
//                .ifEmpty { null }
        )
    }
}
