package csw.services.logging.macros

//TODO: explain better significance
private[logging] object DefaultSourceLocation extends SourceLocation("", "", "", -1)

/**
 * A position in a Scala source file.
 * @param fileName  the name of the file.
 * @param packageName  the package.
 * @param className  the name of the enclosing class.
 * @param line  a line number.
 */
//TODO: explain better significance
case class SourceLocation(fileName: String, packageName: String, className: String, line: Int)
