压缩包是正在写的从 java jar 格式字节码压缩包中提取函数、展开路径并打印的项目源代码，这些打印文件尽可能与 epat++ 研究项目规定的输入格式 .cpath 兼容。

path/ 路径下是打印出的源代码，每个文件夹对应一个函数。目前实验用的 jar 包路径硬编码到一个本地的路径，是从 Apache lucene （https://lucene.apache.org/）的 maven 仓库拿到的，只试着分析了其中的 lucene-analysis-icu-9.9.1.jar 文件。

项目源代码是 kotlin/JVM 的，基于 Soot 提取控制流图，在 PathCondition.kt 中展开，在 DisplayUtils.kt 中进行字符串的后处理，尽可能处理到和 .cpath 格式兼容。目前主要在写字符串处理的部分，争取能自动输出路径来调用 epat++。