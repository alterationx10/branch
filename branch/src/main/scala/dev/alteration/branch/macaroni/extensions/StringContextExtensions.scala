package dev.alteration.branch.macaroni.extensions

object StringContextExtensions {

  extension (sc: StringContext) {

    /** Case-insensitive regex string interpolation
      *
      * @return
      *   A Regex that matches the interpolated string case-insensitively
      */
    def ci = ("(?i)" + sc.parts.mkString).r
  }

}
