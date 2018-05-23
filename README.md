# makeglossariesgui
A Java GUI diagnostic tool for the [glossaries LaTeX package](http://ctan.org/pkg/glossaries). It can check for common problems
that can cause the entries within the glossary or the entire
glossary to be omitted. In GUI mode it parses both the .aux
and the .log file for known error and warning messages.

When run in batch mode, it can be used as alternative to 
the makeglossaries Perl script and the makeglossaries-lite.lua 
Lua script supplied by the [glossaries package](http://ctan.org/pkg/glossaries). (It doesn't parse the log file in batch mode
or show all the diagnostic information that the GUI mode
provides.)

## Third Party Libraries

MakeGlossariesGUI depends on the following third party libraries
whose jar files are placed in the application's lib directory by
the installer:

* [Java Help](https://javahelp.java.net/) GPL
* [The Java Look and Feel Graphics Repository](http://www.oracle.com/technetwork/java/index-138612.html) JLFGR License

If you want to build the application, you will need to fetch those
jar files and add them to the lib directory.

http://www.dickimaw-books.com/
