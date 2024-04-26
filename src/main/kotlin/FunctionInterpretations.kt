import soot.*

sealed interface SExpression {
    fun toStringWithTransformedName(t: (Any) -> String): String
}

class Atom(private val value: Any): SExpression {
    override fun toString(): String {
        return value.toString()
    }

    override fun toStringWithTransformedName(t: (Any) -> String): String {
        return if (value is String) value else t(value)
    }
}

class SList(vararg exps: Any): SExpression {
    private val value = exps.toList().map { if (it is SExpression) it else Atom(it) }
    override fun toString(): String {
        return "(${value.joinToString(" ")})"
    }

    override fun toStringWithTransformedName(t: (Any) -> String): String {
        return "(${value.joinToString(" ") { it.toStringWithTransformedName(t) }})"
    }
}

class TopLevel(private vararg val commands: SExpression): SExpression {
    override fun toString(): String {
        return commands.joinToString("\n")
    }

    override fun toStringWithTransformedName(t: (Any) -> String): String {
        return commands.joinToString("\n") { it.toStringWithTransformedName(t) }
    }
}

//<java.lang.String: void <clinit>()>
//<java.lang.String: void <init>()>
//<java.lang.String: void <init>(java.lang.String)>
//<java.lang.String: void <init>(char[])>
//<java.lang.String: void <init>(char[],int,int)>
//<java.lang.String: void <init>(int[],int,int)>
//<java.lang.String: void <init>(byte[],int,int,int)>
//<java.lang.String: void <init>(byte[],int)>
//<java.lang.String: void checkBounds(byte[],int,int)>
//<java.lang.String: void <init>(byte[],int,int,java.lang.String)>
//<java.lang.String: void <init>(byte[],int,int,java.nio.charset.Charset)>
//<java.lang.String: void <init>(byte[],java.lang.String)>
//<java.lang.String: void <init>(byte[],java.nio.charset.Charset)>
//<java.lang.String: void <init>(byte[],int,int)>
//<java.lang.String: void <init>(byte[])>
//<java.lang.String: void <init>(java.lang.StringBuffer)>
//<java.lang.String: void <init>(java.lang.StringBuilder)>
//<java.lang.String: void <init>(char[],boolean)>
const val length_sig = "<java.lang.String: int length()>"
const val isEmpty_sig = "<java.lang.String: boolean isEmpty()>"
const val charAt_sig = "<java.lang.String: char charAt(int)>"
//<java.lang.String: int codePointAt(int)>
//<java.lang.String: int codePointBefore(int)>
//<java.lang.String: int codePointCount(int,int)>
//<java.lang.String: int offsetByCodePoints(int,int)>
//<java.lang.String: void getChars(char[],int)>
//<java.lang.String: void getChars(int,int,char[],int)>
//<java.lang.String: void getBytes(int,int,byte[],int)>
//<java.lang.String: byte[] getBytes(java.lang.String)>
//<java.lang.String: byte[] getBytes(java.nio.charset.Charset)>
//<java.lang.String: byte[] getBytes()>
//<java.lang.String: boolean equals(java.lang.Object)>
//<java.lang.String: boolean contentEquals(java.lang.StringBuffer)>
//<java.lang.String: boolean nonSyncContentEquals(java.lang.AbstractStringBuilder)>
//<java.lang.String: boolean contentEquals(java.lang.CharSequence)>
//<java.lang.String: boolean equalsIgnoreCase(java.lang.String)>
//<java.lang.String: int compareTo(java.lang.String)>
//<java.lang.String: int compareToIgnoreCase(java.lang.String)>
//<java.lang.String: boolean regionMatches(int,java.lang.String,int,int)>
//<java.lang.String: boolean regionMatches(boolean,int,java.lang.String,int,int)>
const val startsWith0_sig = "<java.lang.String: boolean startsWith(java.lang.String,int)>"
const val startsWith_sig = "<java.lang.String: boolean startsWith(java.lang.String)>"
const val endsWith_sig = "<java.lang.String: boolean endsWith(java.lang.String)>"
//<java.lang.String: int hashCode()>
const val indexOf1_sig = "<java.lang.String: int indexOf(int)>"
const val indexOf2_sig = "<java.lang.String: int indexOf(int,int)>"
//<java.lang.String: int indexOfSupplementary(int,int)>
//<java.lang.String: int lastIndexOf(int)>
//<java.lang.String: int lastIndexOf(int,int)>
//<java.lang.String: int lastIndexOfSupplementary(int,int)>
const val indexOf3_sig = "<java.lang.String: int indexOf(java.lang.String)>"
const val indexOf4_sig = "<java.lang.String: int indexOf(java.lang.String,int)>"
const val indexOf5_sig = "<java.lang.String: int indexOf(int,int,int)>" // modified, for JDK21 seems to have different overloads
const val indexOf6_sig = "<java.lang.String: int indexOf(java.lang.String,int,int)>"
//<java.lang.String: int lastIndexOf(java.lang.String)>
//<java.lang.String: int lastIndexOf(java.lang.String,int)>
//<java.lang.String: int lastIndexOf(char[],int,int,java.lang.String,int)>
//<java.lang.String: int lastIndexOf(char[],int,int,char[],int,int,int)>
const val substring1_sig = "<java.lang.String: java.lang.String substring(int)>"
const val substring2_sig = "<java.lang.String: java.lang.String substring(int,int)>"
//<java.lang.String: java.lang.CharSequence subSequence(int,int)>
const val concat_sig = "<java.lang.String: java.lang.String concat(java.lang.String)>"
//<java.lang.String: java.lang.String replace(char,char)>
//<java.lang.String: boolean matches(java.lang.String)>
const val contains_sig = "<java.lang.String: boolean contains(java.lang.CharSequence)>"
//<java.lang.String: java.lang.String replaceFirst(java.lang.String,java.lang.String)>
//<java.lang.String: java.lang.String replaceAll(java.lang.String,java.lang.String)>
//<java.lang.String: java.lang.String replace(java.lang.CharSequence,java.lang.CharSequence)>
//<java.lang.String: java.lang.String[] split(java.lang.String,int)>
//<java.lang.String: java.lang.String[] split(java.lang.String)>
//<java.lang.String: java.lang.String join(java.lang.CharSequence,java.lang.CharSequence[])>
//<java.lang.String: java.lang.String join(java.lang.CharSequence,java.lang.Iterable)>
//<java.lang.String: java.lang.String toLowerCase(java.util.Locale)>
//<java.lang.String: java.lang.String toLowerCase()>
//<java.lang.String: java.lang.String toUpperCase(java.util.Locale)>
//<java.lang.String: java.lang.String toUpperCase()>
const val trim_sig = "<java.lang.String: java.lang.String trim()>"
//<java.lang.String: java.lang.String toString()>
//<java.lang.String: char[] toCharArray()>
//<java.lang.String: java.lang.String format(java.lang.String,java.lang.Object[])>
//<java.lang.String: java.lang.String format(java.util.Locale,java.lang.String,java.lang.Object[])>
//<java.lang.String: java.lang.String valueOf(java.lang.Object)>
//<java.lang.String: java.lang.String valueOf(char[])>
//<java.lang.String: java.lang.String valueOf(char[],int,int)>
//<java.lang.String: java.lang.String copyValueOf(char[],int,int)>
//<java.lang.String: java.lang.String copyValueOf(char[])>
//<java.lang.String: java.lang.String valueOf(boolean)>
//<java.lang.String: java.lang.String valueOf(char)>
//<java.lang.String: java.lang.String valueOf(int)>
//<java.lang.String: java.lang.String valueOf(long)>
//<java.lang.String: java.lang.String valueOf(float)>
//<java.lang.String: java.lang.String valueOf(double)>
//<java.lang.String: java.lang.String intern()>
//<java.lang.String: int compareTo(java.lang.Object)>

//const val toString_sig = "<java.lang.StringBuffer: java.lang.String toString()>"
//const val append_sig = "<java.lang.StringBuffer: java.lang.StringBuffer append(float)>"
//const val append_sig = "<java.lang.StringBuffer: java.lang.StringBuffer append(double)>"
//const val append_sig = "<java.lang.StringBuffer: java.lang.AbstractStringBuilder append(java.lang.CharSequence)>"
//const val append_sig = "<java.lang.StringBuffer: java.lang.StringBuffer append(boolean)>"
//const val append_sig = "<java.lang.StringBuffer: java.lang.StringBuffer append(char)>"
//const val append_sig = "<java.lang.StringBuffer: java.lang.StringBuffer append(int)>"
//const val append_sig = "<java.lang.StringBuffer: java.lang.AbstractStringBuilder append(java.lang.StringBuffer)>"
//const val append_sig = "<java.lang.StringBuffer: java.lang.StringBuffer append(long)>"
//const val append_sig = "<java.lang.StringBuffer: java.lang.AbstractStringBuilder append(char)>"
//const val append_sig = "<java.lang.StringBuffer: java.lang.AbstractStringBuilder append(int)>"
//const val append_sig = "<java.lang.StringBuffer: java.lang.AbstractStringBuilder append(long)>"
//const val append_sig = "<java.lang.StringBuffer: java.lang.AbstractStringBuilder append(float)>"
//const val append_sig = "<java.lang.StringBuffer: java.lang.AbstractStringBuilder append(double)>"
//const val append_sig = "<java.lang.StringBuffer: java.lang.AbstractStringBuilder append(java.lang.CharSequence,int,int)>"
//const val append_sig = "<java.lang.StringBuffer: java.lang.AbstractStringBuilder append(char[])>"
//const val append_sig = "<java.lang.StringBuffer: java.lang.AbstractStringBuilder append(char[],int,int)>"
//const val append_sig = "<java.lang.StringBuffer: java.lang.AbstractStringBuilder append(boolean)>"
//const val append_sig = "<java.lang.StringBuffer: java.lang.Appendable append(java.lang.CharSequence)>"
//const val append_sig = "<java.lang.StringBuffer: java.lang.AbstractStringBuilder append(java.lang.String)>"
//const val append_sig = "<java.lang.StringBuffer: java.lang.StringBuffer append(java.lang.Object)>"
//const val append_sig = "<java.lang.StringBuffer: java.lang.StringBuffer append(java.lang.String)>"
//const val append_sig = "<java.lang.StringBuffer: java.lang.AbstractStringBuilder append(java.lang.Object)>"
//const val append_sig = "<java.lang.StringBuffer: java.lang.Appendable append(char)>"
//const val append_sig = "<java.lang.StringBuffer: java.lang.Appendable append(java.lang.CharSequence,int,int)>"
//const val append_sig = "<java.lang.StringBuffer: java.lang.StringBuffer append(java.lang.CharSequence)>"
//const val append_sig = "<java.lang.StringBuffer: java.lang.StringBuffer append(java.lang.CharSequence,int,int)>"
//const val append_sig = "<java.lang.StringBuffer: java.lang.StringBuffer append(char[])>"
//const val append_sig = "<java.lang.StringBuffer: java.lang.StringBuffer append(java.lang.StringBuffer)>"
//const val append_sig = "<java.lang.StringBuffer: java.lang.StringBuffer append(char[],int,int)>"
//const val indexOf_sig = "<java.lang.StringBuffer: int indexOf(java.lang.String)>"
//const val indexOf_sig = "<java.lang.StringBuffer: int indexOf(java.lang.String,int)>"
//const val length_sig = "<java.lang.StringBuffer: int length()>"
//const val charAt_sig = "<java.lang.StringBuffer: char charAt(int)>"
//const val codePointAt_sig = "<java.lang.StringBuffer: int codePointAt(int)>"
//const val codePointBefore_sig = "<java.lang.StringBuffer: int codePointBefore(int)>"
//const val codePointCount_sig = "<java.lang.StringBuffer: int codePointCount(int,int)>"
//const val offsetByCodePoints_sig = "<java.lang.StringBuffer: int offsetByCodePoints(int,int)>"
//const val getChars_sig = "<java.lang.StringBuffer: void getChars(int,int,char[],int)>"
//const val lastIndexOf_sig = "<java.lang.StringBuffer: int lastIndexOf(java.lang.String,int)>"
//const val lastIndexOf_sig = "<java.lang.StringBuffer: int lastIndexOf(java.lang.String)>"
//const val substring_sig = "<java.lang.StringBuffer: java.lang.String substring(int)>"
//const val substring_sig = "<java.lang.StringBuffer: java.lang.String substring(int,int)>"
//const val subSequence_sig = "<java.lang.StringBuffer: java.lang.CharSequence subSequence(int,int)>"
//const val replace_sig = "<java.lang.StringBuffer: java.lang.AbstractStringBuilder replace(int,int,java.lang.String)>"
//const val replace_sig = "<java.lang.StringBuffer: java.lang.StringBuffer replace(int,int,java.lang.String)>"
//const val delete_sig = "<java.lang.StringBuffer: java.lang.AbstractStringBuilder delete(int,int)>"
//const val delete_sig = "<java.lang.StringBuffer: java.lang.StringBuffer delete(int,int)>"
//const val insert_sig = "<java.lang.StringBuffer: java.lang.AbstractStringBuilder insert(int,char[])>"
//const val insert_sig = "<java.lang.StringBuffer: java.lang.AbstractStringBuilder insert(int,java.lang.CharSequence)>"
//const val insert_sig = "<java.lang.StringBuffer: java.lang.AbstractStringBuilder insert(int,java.lang.CharSequence,int,int)>"
//const val insert_sig = "<java.lang.StringBuffer: java.lang.AbstractStringBuilder insert(int,boolean)>"
//const val insert_sig = "<java.lang.StringBuffer: java.lang.AbstractStringBuilder insert(int,java.lang.String)>"
//const val insert_sig = "<java.lang.StringBuffer: java.lang.AbstractStringBuilder insert(int,java.lang.Object)>"
//const val insert_sig = "<java.lang.StringBuffer: java.lang.AbstractStringBuilder insert(int,char[],int,int)>"
//const val insert_sig = "<java.lang.StringBuffer: java.lang.StringBuffer insert(int,java.lang.CharSequence)>"
//const val insert_sig = "<java.lang.StringBuffer: java.lang.StringBuffer insert(int,java.lang.CharSequence,int,int)>"
//const val insert_sig = "<java.lang.StringBuffer: java.lang.StringBuffer insert(int,boolean)>"
//const val insert_sig = "<java.lang.StringBuffer: java.lang.StringBuffer insert(int,char)>"
//const val insert_sig = "<java.lang.StringBuffer: java.lang.StringBuffer insert(int,int)>"
//const val insert_sig = "<java.lang.StringBuffer: java.lang.StringBuffer insert(int,float)>"
//const val insert_sig = "<java.lang.StringBuffer: java.lang.StringBuffer insert(int,double)>"
//const val insert_sig = "<java.lang.StringBuffer: java.lang.StringBuffer insert(int,char[],int,int)>"
//const val insert_sig = "<java.lang.StringBuffer: java.lang.StringBuffer insert(int,java.lang.Object)>"
//const val insert_sig = "<java.lang.StringBuffer: java.lang.StringBuffer insert(int,java.lang.String)>"
//const val insert_sig = "<java.lang.StringBuffer: java.lang.StringBuffer insert(int,char[])>"
//const val insert_sig = "<java.lang.StringBuffer: java.lang.AbstractStringBuilder insert(int,double)>"
//const val insert_sig = "<java.lang.StringBuffer: java.lang.AbstractStringBuilder insert(int,float)>"
//const val insert_sig = "<java.lang.StringBuffer: java.lang.AbstractStringBuilder insert(int,long)>"
//const val insert_sig = "<java.lang.StringBuffer: java.lang.AbstractStringBuilder insert(int,int)>"
//const val insert_sig = "<java.lang.StringBuffer: java.lang.AbstractStringBuilder insert(int,char)>"
//const val insert_sig = "<java.lang.StringBuffer: java.lang.StringBuffer insert(int,long)>"
//const val capacity_sig = "<java.lang.StringBuffer: int capacity()>"
//const val ensureCapacity_sig = "<java.lang.StringBuffer: void ensureCapacity(int)>"
//const val trimToSize_sig = "<java.lang.StringBuffer: void trimToSize()>"
//const val setLength_sig = "<java.lang.StringBuffer: void setLength(int)>"
//const val setCharAt_sig = "<java.lang.StringBuffer: void setCharAt(int,char)>"
//const val appendCodePoint_sig = "<java.lang.StringBuffer: java.lang.StringBuffer appendCodePoint(int)>"
//const val appendCodePoint_sig = "<java.lang.StringBuffer: java.lang.AbstractStringBuilder appendCodePoint(int)>"
//const val deleteCharAt_sig = "<java.lang.StringBuffer: java.lang.AbstractStringBuilder deleteCharAt(int)>"
//const val deleteCharAt_sig = "<java.lang.StringBuffer: java.lang.StringBuffer deleteCharAt(int)>"
//const val reverse_sig = "<java.lang.StringBuffer: java.lang.StringBuffer reverse()>"
//const val reverse_sig = "<java.lang.StringBuffer: java.lang.AbstractStringBuilder reverse()>"
//const val wait_sig = "<java.lang.StringBuffer: void wait(long,int)>"
//const val wait_sig = "<java.lang.StringBuffer: void wait(long)>"
//const val wait_sig = "<java.lang.StringBuffer: void wait()>"
//const val equals_sig = "<java.lang.StringBuffer: boolean equals(java.lang.Object)>"
//const val hashCode_sig = "<java.lang.StringBuffer: int hashCode()>"
//const val getClass_sig = "<java.lang.StringBuffer: java.lang.Class<?> getClass()>"
//const val notify_sig = "<java.lang.StringBuffer: void notify()>"
//const val notifyAll_sig = "<java.lang.StringBuffer: void notifyAll()>"
//const val chars_sig = "<java.lang.StringBuffer: java.util.stream.IntStream chars()>"
//const val codePoints_sig = "<java.lang.StringBuffer: java.util.stream.IntStream codePoints()>"


const val sb_init_sig = "<java.lang.StringBuilder: void <init>(java.lang.String)>"
const val sb_toString_sig = "<java.lang.StringBuilder: java.lang.String toString()>"
//const val append_sig = "<java.lang.StringBuilder: java.lang.AbstractStringBuilder append(long)>"
//const val append_sig = "<java.lang.StringBuilder: java.lang.AbstractStringBuilder append(int)>"
//const val append_sig = "<java.lang.StringBuilder: java.lang.AbstractStringBuilder append(char)>"
//const val append_sig = "<java.lang.StringBuilder: java.lang.AbstractStringBuilder append(boolean)>"
//const val append_sig = "<java.lang.StringBuilder: java.lang.AbstractStringBuilder append(java.lang.CharSequence,int,int)>"
//const val append_sig = "<java.lang.StringBuilder: java.lang.Appendable append(java.lang.CharSequence)>"
//const val append_sig = "<java.lang.StringBuilder: java.lang.AbstractStringBuilder append(double)>"
//const val append_sig = "<java.lang.StringBuilder: java.lang.AbstractStringBuilder append(float)>"
//const val append_sig = "<java.lang.StringBuilder: java.lang.AbstractStringBuilder append(java.lang.StringBuffer)>"
//const val append_sig = "<java.lang.StringBuilder: java.lang.AbstractStringBuilder append(java.lang.String)>"
//const val append_sig = "<java.lang.StringBuilder: java.lang.AbstractStringBuilder append(java.lang.Object)>"
//const val append_sig = "<java.lang.StringBuilder: java.lang.Appendable append(char)>"
//const val append_sig = "<java.lang.StringBuilder: java.lang.AbstractStringBuilder append(char[],int,int)>"
//const val append_sig = "<java.lang.StringBuilder: java.lang.AbstractStringBuilder append(char[])>"
//const val append_sig = "<java.lang.StringBuilder: java.lang.Appendable append(java.lang.CharSequence,int,int)>"
//const val append_sig = "<java.lang.StringBuilder: java.lang.AbstractStringBuilder append(java.lang.CharSequence)>"
//const val append_sig = "<java.lang.StringBuilder: java.lang.StringBuilder append(boolean)>"
//const val append_sig = "<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.CharSequence)>"
//const val append_sig = "<java.lang.StringBuilder: java.lang.StringBuilder append(char)>"
//const val append_sig = "<java.lang.StringBuilder: java.lang.StringBuilder append(int)>"
//const val append_sig = "<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.StringBuffer)>"
//const val append_sig = "<java.lang.StringBuilder: java.lang.StringBuilder append(char[])>"
//const val append_sig = "<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.CharSequence,int,int)>"
//const val append_sig = "<java.lang.StringBuilder: java.lang.StringBuilder append(double)>"
//const val append_sig = "<java.lang.StringBuilder: java.lang.StringBuilder append(char[],int,int)>"
const val append_sig = "<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.String)>"
//const val append_sig = "<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.Object)>"
//const val append_sig = "<java.lang.StringBuilder: java.lang.StringBuilder append(long)>"
//const val append_sig = "<java.lang.StringBuilder: java.lang.StringBuilder append(float)>"
//const val indexOf_sig = "<java.lang.StringBuilder: int indexOf(java.lang.String,int)>"
//const val indexOf_sig = "<java.lang.StringBuilder: int indexOf(java.lang.String)>"
//const val length_sig = "<java.lang.StringBuilder: int length()>"
//const val charAt_sig = "<java.lang.StringBuilder: char charAt(int)>"
//const val codePointAt_sig = "<java.lang.StringBuilder: int codePointAt(int)>"
//const val codePointBefore_sig = "<java.lang.StringBuilder: int codePointBefore(int)>"
//const val codePointCount_sig = "<java.lang.StringBuilder: int codePointCount(int,int)>"
//const val offsetByCodePoints_sig = "<java.lang.StringBuilder: int offsetByCodePoints(int,int)>"
//const val getChars_sig = "<java.lang.StringBuilder: void getChars(int,int,char[],int)>"
//const val lastIndexOf_sig = "<java.lang.StringBuilder: int lastIndexOf(java.lang.String,int)>"
//const val lastIndexOf_sig = "<java.lang.StringBuilder: int lastIndexOf(java.lang.String)>"
//const val substring_sig = "<java.lang.StringBuilder: java.lang.String substring(int)>"
//const val substring_sig = "<java.lang.StringBuilder: java.lang.String substring(int,int)>"
//const val subSequence_sig = "<java.lang.StringBuilder: java.lang.CharSequence subSequence(int,int)>"
//const val replace_sig = "<java.lang.StringBuilder: java.lang.AbstractStringBuilder replace(int,int,java.lang.String)>"
//const val replace_sig = "<java.lang.StringBuilder: java.lang.StringBuilder replace(int,int,java.lang.String)>"
//const val delete_sig = "<java.lang.StringBuilder: java.lang.AbstractStringBuilder delete(int,int)>"
//const val delete_sig = "<java.lang.StringBuilder: java.lang.StringBuilder delete(int,int)>"
//const val insert_sig = "<java.lang.StringBuilder: java.lang.StringBuilder insert(int,char[],int,int)>"
//const val insert_sig = "<java.lang.StringBuilder: java.lang.AbstractStringBuilder insert(int,long)>"
//const val insert_sig = "<java.lang.StringBuilder: java.lang.StringBuilder insert(int,double)>"
//const val insert_sig = "<java.lang.StringBuilder: java.lang.StringBuilder insert(int,float)>"
//const val insert_sig = "<java.lang.StringBuilder: java.lang.StringBuilder insert(int,long)>"
//const val insert_sig = "<java.lang.StringBuilder: java.lang.StringBuilder insert(int,int)>"
//const val insert_sig = "<java.lang.StringBuilder: java.lang.StringBuilder insert(int,java.lang.Object)>"
//const val insert_sig = "<java.lang.StringBuilder: java.lang.AbstractStringBuilder insert(int,double)>"
//const val insert_sig = "<java.lang.StringBuilder: java.lang.AbstractStringBuilder insert(int,float)>"
//const val insert_sig = "<java.lang.StringBuilder: java.lang.StringBuilder insert(int,java.lang.String)>"
//const val insert_sig = "<java.lang.StringBuilder: java.lang.StringBuilder insert(int,char[])>"
//const val insert_sig = "<java.lang.StringBuilder: java.lang.StringBuilder insert(int,java.lang.CharSequence)>"
//const val insert_sig = "<java.lang.StringBuilder: java.lang.StringBuilder insert(int,java.lang.CharSequence,int,int)>"
//const val insert_sig = "<java.lang.StringBuilder: java.lang.StringBuilder insert(int,boolean)>"
//const val insert_sig = "<java.lang.StringBuilder: java.lang.StringBuilder insert(int,char)>"
//const val insert_sig = "<java.lang.StringBuilder: java.lang.AbstractStringBuilder insert(int,boolean)>"
//const val insert_sig = "<java.lang.StringBuilder: java.lang.AbstractStringBuilder insert(int,java.lang.CharSequence,int,int)>"
//const val insert_sig = "<java.lang.StringBuilder: java.lang.AbstractStringBuilder insert(int,java.lang.CharSequence)>"
//const val insert_sig = "<java.lang.StringBuilder: java.lang.AbstractStringBuilder insert(int,char[])>"
//const val insert_sig = "<java.lang.StringBuilder: java.lang.AbstractStringBuilder insert(int,java.lang.Object)>"
//const val insert_sig = "<java.lang.StringBuilder: java.lang.AbstractStringBuilder insert(int,java.lang.String)>"
//const val insert_sig = "<java.lang.StringBuilder: java.lang.AbstractStringBuilder insert(int,char[],int,int)>"
//const val insert_sig = "<java.lang.StringBuilder: java.lang.AbstractStringBuilder insert(int,char)>"
//const val insert_sig = "<java.lang.StringBuilder: java.lang.AbstractStringBuilder insert(int,int)>"
//const val capacity_sig = "<java.lang.StringBuilder: int capacity()>"
//const val ensureCapacity_sig = "<java.lang.StringBuilder: void ensureCapacity(int)>"
//const val trimToSize_sig = "<java.lang.StringBuilder: void trimToSize()>"
//const val setLength_sig = "<java.lang.StringBuilder: void setLength(int)>"
//const val setCharAt_sig = "<java.lang.StringBuilder: void setCharAt(int,char)>"
//const val appendCodePoint_sig = "<java.lang.StringBuilder: java.lang.AbstractStringBuilder appendCodePoint(int)>"
//const val appendCodePoint_sig = "<java.lang.StringBuilder: java.lang.StringBuilder appendCodePoint(int)>"
//const val deleteCharAt_sig = "<java.lang.StringBuilder: java.lang.AbstractStringBuilder deleteCharAt(int)>"
//const val deleteCharAt_sig = "<java.lang.StringBuilder: java.lang.StringBuilder deleteCharAt(int)>"
//const val reverse_sig = "<java.lang.StringBuilder: java.lang.AbstractStringBuilder reverse()>"
//const val reverse_sig = "<java.lang.StringBuilder: java.lang.StringBuilder reverse()>"
//const val wait_sig = "<java.lang.StringBuilder: void wait(long,int)>"
//const val wait_sig = "<java.lang.StringBuilder: void wait(long)>"
//const val wait_sig = "<java.lang.StringBuilder: void wait()>"
//const val equals_sig = "<java.lang.StringBuilder: boolean equals(java.lang.Object)>"
//const val hashCode_sig = "<java.lang.StringBuilder: int hashCode()>"
//const val getClass_sig = "<java.lang.StringBuilder: java.lang.Class<?> getClass()>"
//const val notify_sig = "<java.lang.StringBuilder: void notify()>"
//const val notifyAll_sig = "<java.lang.StringBuilder: void notifyAll()>"
//const val chars_sig = "<java.lang.StringBuilder: java.util.stream.IntStream chars()>"
//const val codePoints_sig = "<java.lang.StringBuilder: java.util.stream.IntStream codePoints()>"

const val cs_toString_sig = "<java.lang.CharSequence: java.lang.String toString()>"
const val cs_length_sig = "<java.lang.CharSequence: int length()>"
const val cs_charAt_sig = "<java.lang.CharSequence: char charAt(int)>"
const val cs_subSequence_sig = "<java.lang.CharSequence: java.lang.CharSequence subSequence(int,int)>"
const val cs_chars_sig = "<java.lang.CharSequence: java.util.stream.IntStream chars()>"
const val cs_codePoints_sig = "<java.lang.CharSequence: java.util.stream.IntStream codePoints()>"

const val next_sig = "<java.util.Iterator: java.lang.Object next()>"

fun predefineFunctions(functions: MutableMap<String, Pair<List<Any>, Any>>): List<SExpression> {

    fun listOfStringApis(): Map<String, SExpression> {
        // also refer to https://github.com/jiaxy/jconcolic/blob/master/jconcolic-core/src/main/java/edu/whu/jconcolic/solver/SMT2Visitor.java#L392
        val funcs = mutableMapOf<String, SExpression>()

        funcs["length/${length_sig.hashCode()}"] = SList(
            "define-fun",
            "length/${length_sig.hashCode()}",
            SList(
                SList("s", "String")
            ),
            "Int",
            SList("str.len", "s")
        )

        funcs["charAt/${charAt_sig.hashCode()}"] = SList(
            "define-fun",
            "charAt/${charAt_sig.hashCode()}",
            SList(
                SList("s", "String"),
                SList("index", "Int")
            ),
            "Int",
            SList(
                "str.to_code",
                SList(
                    "str.at",
                    "s",
                    "index"
                )
            )
        )

        funcs["isEmpty/${isEmpty_sig.hashCode()}"] = SList(
            "define-fun",
            "isEmpty/${isEmpty_sig.hashCode()}",
            SList(
                SList("s", "String")
            ),
            "Bool",
            SList(
                "=",
                SList("str.len", "s"),
                "0"
            )
        )

        funcs["startsWith/${startsWith_sig.hashCode()}"] = SList(
            "define-fun",
            "startsWith/${startsWith_sig.hashCode()}",
            SList(
                SList("s", "String"),
                SList("prefix", "String")
            ),
            "Bool",
            SList(
                "str.prefixof",
                "prefix",
                "s"
            )
        )

        funcs["startsWith/${startsWith0_sig.hashCode()}"] = SList(
            "define-fun",
            "startsWith/${startsWith0_sig.hashCode()}",
            SList(
                SList("s", "String"),
                SList("prefix", "String"),
                SList("toffset", "Int")
            ),
            "Bool",
            SList(
                "and",
                SList(
                    ">=",
                    "toffset",
                    "0"
                ),
                SList(
                    ">=",
                    SList(
                        "str.len",
                        "s"
                    ),
                    "toffset"
                ),
                SList(
                    "str.prefixof",
                    "prefix",
                    SList(
                        "str.substr",
                        "s",
                        "toffset",
                        SList(
                            "-",
                            SList(
                                "str.len",
                                "s"
                            ),
                            "toffset"
                        )
                    )
                )
            )
        )

        funcs["endsWith/${endsWith_sig.hashCode()}"] = SList(
            "define-fun",
            "endsWith/${endsWith_sig.hashCode()}",
            SList(
                SList("s", "String"),
                SList("suffix", "String")
            ),
            "Bool",
            SList(
                "str.suffixof",
                "suffix",
                "s"
            )
        )

        funcs["indexOf/${indexOf1_sig.hashCode()}"] = SList(
            "define-fun",
            "indexOf/${indexOf1_sig.hashCode()}",
            SList(
                SList("s", "String"),
                SList("c", "Int")
            ),
            "Int",
            SList(
                "str.indexof",
                "s",
                SList(
                    "str.from_code",
                    "c"
                ),
                "0"
            )
        )

        funcs["indexOf/${indexOf2_sig.hashCode()}"] = SList(
            "define-fun",
            "indexOf/${indexOf2_sig.hashCode()}",
            SList(
                SList("s", "String"),
                SList("c", "Int"),
                SList("fromIndex", "Int")
            ),
            "Int",
            SList(
                "str.indexof",
                "s",
                SList(
                    "str.from_code",
                    "c"
                ),
                "fromIndex"
            )
        )

        funcs["indexOf/${indexOf3_sig.hashCode()}"] = SList(
            "define-fun",
            "indexOf/${indexOf3_sig.hashCode()}",
            SList(
                SList("s", "String"),
                SList("subs", "String")
            ),
            "Int",
            SList(
                "str.indexof",
                "s",
                "subs",
                "0"
            )
        )

        funcs["indexOf/${indexOf4_sig.hashCode()}"] = SList(
            "define-fun",
            "indexOf/${indexOf4_sig.hashCode()}",
            SList(
                SList("s", "String"),
                SList("subs", "String"),
                SList("fromIndex", "Int")
            ),
            "Int",
            SList(
                "str.indexof",
                "s",
                "subs",
                "fromIndex"
            )
        )

        funcs["indexOf/${indexOf5_sig.hashCode()}"] = SList(
            "define-fun",
            "indexOf/${indexOf5_sig.hashCode()}",
            SList(
                SList("s", "String"),
                SList("c", "int"),
                SList("beginIndex", "Int"),
                SList("endIndex", "Int")
            ),
            "Int",
            SList(
                "+",
                "beginIndex",
                SList(
                    "str.indexof",
                    SList(
                        "str.substr",
                        "s",
                        "beginIndex",
                        SList(
                            "-",
                            "endIndex",
                            "beginIndex"
                        )
                    ),
                    SList(
                        "str.from_code",
                        "c"
                    ),
                    "0"
                )
            )
        )

        funcs["indexOf/${indexOf6_sig.hashCode()}"] = SList(
            "define-fun",
            "indexOf/${indexOf6_sig.hashCode()}",
            SList(
                SList("s", "String"),
                SList("subs", "String"),
                SList("beginIndex", "Int"),
                SList("endIndex", "Int")
            ),
            "Int",
            SList(
                "+",
                "beginIndex",
                SList(
                    "str.indexof",
                    SList(
                        "str.substr",
                        "s",
                        "beginIndex",
                        SList(
                            "-",
                            "endIndex",
                            "beginIndex"
                        )
                    ),
                    "subs",
                    "0"
                )
            )
        )

        funcs["contains/${contains_sig.hashCode()}"] = SList(
            "define-fun",
            "contains/${contains_sig.hashCode()}",
            SList(
                SList("s", "String"),
                SList("subs", "String")
            ),
            "Bool",
            SList(
                "str.contains",
                "s",
                "subs"
            )
        )
        funcs["concat/${concat_sig.hashCode()}"] = SList(
            "define-fun",
            "concat/${concat_sig.hashCode()}",
            SList(
                SList("s", "String"),
                SList("next", "String")
            ),
            "String",
            SList(
                "str.++",
                "s",
                "next"
            )
        )

        funcs["substring/${substring2_sig.hashCode()}"] = SList(
            "define-fun",
            "substring/${substring2_sig.hashCode()}",
            SList(
                SList("s", "String"),
                SList("begin", "Int"),
                SList("end", "Int")
            ),
            "String",
            SList(
                "str.substr",
                "s",
                "begin",
                SList(
                    "-",
                    "end",
                    "begin"
                )
            )
        )

        funcs["substring/${substring1_sig.hashCode()}"] = SList(
            "define-fun",
            "substring/${substring1_sig.hashCode()}",
            SList(
                SList("s", "String"),
                SList("begin", "Int")
            ),
            "String",
            SList(
                "str.substr",
                "s",
                "begin",
                SList(
                    "-",
                    SList(
                        "str.len",
                        "s"
                    ),
                    "begin"
                )
            )
        )

        // refer to but later changed from:
        // https://github.com/yanxx297/jpf-symbc/blob/9fa4eff5d25b5d29919a64432422f912dff0a609/doc/trim.smt#L4
        // or https://github.com/yanxx297/jpf-symbc/blob/9fa4eff5d25b5d29919a64432422f912dff0a609/src/main/edu/ucsb/cs/vlab/translate/smtlib/from/z3str3/Z3Translator.java#L265
        // to approximate standard java:
        // https://github.com/openjdk/jdk/blob/master/src/java.base/share/classes/java/lang/String.java
        // https://github.com/openjdk/jdk/blob/master/src/java.base/share/classes/java/lang/StringUTF16.java#L75
        // https://github.com/openjdk/jdk/blob/master/src/java.base/share/classes/java/lang/StringLatin1.java#L856
        funcs["trim/${trim_sig.hashCode()}"] = TopLevel(
            SList(
                "define-fun",
                "is-whitespace",
                SList(
                    SList(
                        "char",
                        "String"
                    )
                ),
                "Bool",
                SList(
                    "<",
                    SList(
                        "str.to_code",
                        "char"
                    ),
                    "33" // ASCII space
                )
            ),

            SList(
                "define-fun-rec",
                "trim-left",  // TODO: temporarily only deal with ASCII characters
                // also, use the recursive version might cause performance issues
                SList(
                    SList(
                        "s",
                        "String"
                    )
                ),
                "String",
                SList(
                    "ite",
                    SList(
                        "=",
                        "s",
                        "\"\""
                    ),
                    "\"\"",
                    SList(
                        "ite",
                        SList(
                            "is-whitespace",
                            SList(
                                "str.substr",
                                "s",
                                "0",
                                "1"
                            )
                        ),
                        SList(
                            "trim-left",
                            SList(
                                "str.substr",
                                "s",
                                "1",
                                SList(
                                    "-",
                                    SList(
                                        "str.len",
                                        "s"
                                    ),
                                    "1"
                                )
                            )
                        ),
                        "s"
                    )
                )
            ),
            SList(
                "define-fun-rec",
                "trim-right",
                SList(
                    SList(
                        "s",
                        "String"
                    )
                ),
                "String",
                SList(
                    "ite",
                    SList(
                        "=",
                        "s",
                        "\"\""
                    ),
                    "\"\"",
                    SList(
                        "ite",
                        SList(
                            "is-whitespace",
                            SList(
                                "str.substr",
                                "s",
                                SList(
                                    "-",
                                    SList(
                                        "str.len",
                                        "s"
                                    ),
                                    "1"
                                ),
                                "1"
                            )
                        ),
                        SList(
                            "trim-right",
                            SList(
                                "str.substr",
                                "s",
                                "0",
                                SList(
                                    "-",
                                    SList(
                                        "str.len",
                                        "s"
                                    ),
                                    "1"
                                )
                            )
                        ),
                        "s"
                    )
                )
            ),
            SList(
                "define-fun",
                "trim/${trim_sig.hashCode()}",
                SList(
                    SList(
                        "s",
                        "String"
                    )
                ),
                "String",
                SList(
                    "trim-right",
                    SList(
                        "trim-left",
                        "s"
                    )
                )
            )
        )

        funcs["<init>/${sb_init_sig.hashCode()}"] = SList(
            "define-fun",
            "<init>/${sb_init_sig.hashCode()}",
            SList(
                SList("s", "String")
            ),
            "String",
            "s"
        )

        funcs["append/${append_sig.hashCode()}"] = SList(
            "define-fun",
            "append/${append_sig.hashCode()}",
            SList(
                SList("s", "String"),
                SList("tail", "String")
            ),
            "String",
            SList(
                "str.++",
                "s",
                "tail"
            )
        )

        funcs["toString/${sb_toString_sig.hashCode()}"] = SList(
            "define-fun",
            "toString/${sb_toString_sig.hashCode()}",
            SList(
                SList("s", "String")
            ),
            "String",
            "s"
        )

        funcs["length/${cs_length_sig.hashCode()}"] = SList(
            "define-fun",
            "length/${cs_length_sig.hashCode()}",
            SList(
                SList("s", "String")
            ),
            "Int",
            SList("str.len", "s")
        )

        funcs["toString/${cs_toString_sig.hashCode()}"] = SList(
            "define-fun",
            "toString/${cs_toString_sig.hashCode()}",
            SList(
                SList("s", "String")
            ),
            "String",
            "s"
        )

        funcs["charAt/${cs_charAt_sig.hashCode()}"] = SList(
            "define-fun",
            "charAt/${cs_charAt_sig.hashCode()}",
            SList(
                SList("s", "String"),
                SList("index", "Int")
            ),
            "Int",
            SList(
                "str.to_code",
                SList(
                    "str.at",
                    "s",
                    "index"
                )
            )
        )

        funcs["subSequence/${cs_subSequence_sig.hashCode()}"] = SList(
            "define-fun",
            "subSequence/${cs_subSequence_sig.hashCode()}",
            SList(
                SList("s", "String"),
                SList("begin", "Int"),
                SList("end", "Int")
            ),
            "String",
            SList(
                "str.substr",
                "s",
                "begin",
                SList(
                    "-",
                    "end",
                    "begin"
                )
            )
        )

//        funcs["chars/${cs_chars_sig.hashCode()}"] = SList(
//            "define-fun",
//            "chars/${cs_chars_sig.hashCode()}",
//            SList(
//                SList("s", "CharSequence")
//            ),
//            "java.util.stream.IntStream",
//            SList("str.chars", "s")
//        )

//        funcs["codePoints/${cs_codePoints_sig.hashCode()}"] = SList(
//            "define-fun",
//            "codePoints/${cs_codePoints_sig.hashCode()}",
//            SList(
//                SList("s", "CharSequence")
//            ),
//            "java.util.stream.IntStream",
//            SList("str.codepoints", "s")
//        )

        return funcs
    }

    fun trivialCasts(name: String, types: Pair<List<Any>, Any>): SList? {
        try {
            val (ty1, ty2) = name.removePrefix("cast-from-").split("-to-")
            if (ty1 == ty2)
                if (types.first.size == 1)
                    return SList(
                        "define-fun",
                        name,
                        SList(
                            SList("arg", types.first[0])
                        ),
                        types.second,
                        "arg"
                    )
        } catch (_: Throwable) {} finally {}
        return null
    }

    val funcs = listOfStringApis()
    // only the used functions of above (as well as their helpers) are included
    return functions.map { (name, types) ->
        funcs[name] ?: trivialCasts(name, types) ?: SList(
            "declare-fun",
            name,
            SList(
                *types.first.toTypedArray()
            ),
            types.second
        )
    }
}

fun preconditionOfFunctions(name: String, args: List<String>): SExpression? {
    return when (name) {
        "substring/${substring2_sig.hashCode()}" -> {
            val s = args[0]
            val begin = args[1]
            val end = args[2]
            SList(
                "and",
                SList(
                    ">=",
                    begin,
                    "0"
                ),
                SList(
                    ">=",
                    SList(
                        "str.len",
                        s
                    ),
                    end
                ),
                SList(
                    ">=",
                    end,
                    begin
                )
            )
        }
        "indexOf/${indexOf5_sig.hashCode()}", "indexOf/${indexOf6_sig.hashCode()}" -> {
            val s = args[0]
            val begin = args[2]
            val end = args[3]
            SList(
                "and",
                SList(
                    ">=",
                    begin,
                    "0"
                ),
                SList(
                    ">=",
                    SList(
                        "str.len",
                        s
                    ),
                    end
                ),
                SList(
                    ">=",
                    end,
                    begin
                )
            )
        }
        "substring/${substring1_sig.hashCode()}", "subSequence/${cs_subSequence_sig.hashCode()}" -> {
            val s = args[0]
            val begin = args[1]
            SList(
                "and",
                SList(
                    ">=",
                    begin,
                    "0"
                ),
                SList(
                    ">=",
                    SList(
                        "str.len",
                        s
                    ),
                    begin
                )
            )
        }
        "charAt/${charAt_sig.hashCode()}", "charAt/${cs_charAt_sig.hashCode()}" -> {
            val s = args[0]
            val index = args[1]
            SList(
                "and",
                SList(
                    ">",
                    SList(
                        "str.len",
                        s
                    ),
                    index
                ),
                SList(
                    "<=",
                    "0",
                    index
                )
            )
        }
        else -> null
    }
}

// TODO: remove soot dependency in this file at best effort
// post condition might produce new constants as some of the objects might be re-assigned, which cannot be predicted,
// so the function needs to know how to produce one by applying `getNewName` to one of the args
inline fun postconditionOfFunctions(funcName: String, args: List<Value>, getName: (Value) -> String, addReDeclarationOf: (Value) -> String): SExpression? {
    return when (funcName) {
        "next/${next_sig.hashCode()}" -> {
            val iteratorObject = args[0]
            val iteratorName = getName(iteratorObject)
            TopLevel( // TODO: fix the assertions
                Atom(addReDeclarationOf(iteratorObject)), // work around, use `transformDefine()` to add re-declaration
                                                // for consistency and modularity, so the product is a string like
                                                // "(declare-const A B)", and can be directly inserted into the TopLevel
                                                // without SList's adding parentheses
//                SList(
//                    "assert",
//                    SList()
//                )
            )
        }

        "<init>/${sb_init_sig.hashCode()}" -> {
            val sbObject = args[0]
            val initializer = args[1]
            val oldName = getName(sbObject)
            TopLevel(
                Atom(addReDeclarationOf(sbObject)),
                SList(
                    "assert",
                    SList(
                        "=",
                        sbObject,
                        initializer
                    )
                )
            )
        }

        "append/${append_sig.hashCode()}" -> {
            val sbObject = args[0]
            val tail = args[1]
            val oldName = getName(sbObject)
            TopLevel(
                Atom(addReDeclarationOf(sbObject)),
                SList(
                    "assert",
                    SList(
                        "=",
                        sbObject,
                        SList(
                            "str.++",
                            oldName,
                            tail
                        )
                    )
                )
            )
        }

        else -> null
    }
}

fun isNotSameTypeButCastable(subType: Type, topType: Type, strict: Boolean = false): Boolean =
    (subType is RefType && topType is RefType && subType.merge(topType, Scene.v()) != subType) || // true subtype
            (topType == RefType.v("java.lang.String") && !strict) || // xxx.toString() method
            (topType is ArrayType && subType is ArrayType && isNotSameTypeButCastable(subType.baseType, topType.baseType)) || // array of subtype
            (subType is ArrayType && topType == RefType.v("java.lang.Object")) || // arrays are sub of Object
            (topType == RefType.v("java.util.Collection") && subType.toString().contains("(List|Array|Map)".toRegex())) // collections
