# jEdit Beanshell Debugger Plugin

The Plugin provides the ability to debug beanshell scripts inside jEdit. 
Discussion in jEdit forum https://sourceforge.net/p/jedit/patches/576/

# Installation
Copy the jEdit_plugin_jar/bshdebugger.jar file to the jEdit jars folder.

# How to debug a beanshell script
1. Open your bsh file
2. Set a breakpoint via editor context menu "Toggle Beanshell Breakpoint" or plugin menu "Plugins->Beanshell Debugger->Toggle Beanshell Breakpoint"
3. Open Beanshell Debug view via plugin menu "Plugins->Beanshell Debugger->Open Beanshell Debug view"
4. run your beanshell macro and enjoy debugging ... 

# Known issues
1. debugging only on method block level supported
2. only simple step implemented; step over, step out missing

