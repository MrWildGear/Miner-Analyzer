# Java Plugins Guide for Cursor/VS Code

This guide lists the recommended Java extensions/plugins for developing this project in Cursor (or VS Code).

## 🚀 Essential Extensions (Install These First)

### 1. **Extension Pack for Java** (Microsoft)
   - **Extension ID**: `vscjava.vscode-java-pack`
   - **What it includes**: This is a bundle that includes:
     - Language Support for Java by Red Hat
     - Debugger for Java
     - Test Runner for Java
     - Maven for Java
     - Project Manager for Java
     - Visual Studio IntelliCode
   - **Why you need it**: This is the most important extension - it provides core Java language support, debugging, and project management.

### 2. **SonarLint** (SonarSource)
   - **Extension ID**: `SonarSource.sonarlint-vscode`
   - **What it does**: Real-time code quality and security analysis
   - **Why you need it**: Catches bugs, code smells, and security vulnerabilities as you type

## 📦 Additional Recommended Extensions

### 3. **Gradle for Java** (Microsoft)
   - **Extension ID**: `vscjava.vscode-gradle`
   - **What it does**: Gradle build tool support
   - **Note**: Currently you're using batch scripts, but this is useful if you migrate to Gradle

### 4. **Java Dependency Viewer** (Microsoft)
   - **Extension ID**: `vscjava.vscode-java-dependency`
   - **What it does**: Visualize and manage Java project dependencies
   - **Why it's useful**: Helps understand project structure and dependencies

## 🔧 How to Install

### Option 1: Install via Cursor/VS Code UI
1. Open Cursor/VS Code
2. Press `Ctrl+Shift+X` (or `Cmd+Shift+X` on Mac) to open Extensions view
3. Search for "Extension Pack for Java" and click Install
4. Search for "SonarLint" and click Install

### Option 2: Install via Command Line
```bash
code --install-extension vscjava.vscode-java-pack
code --install-extension SonarSource.sonarlint-vscode
```

### Option 3: Auto-install (Recommended)
The `.vscode/extensions.json` file in this project will prompt you to install recommended extensions when you open the project.

## ⚙️ Configuration

The project includes a `.vscode/settings.json` file that configures:
- Java 22 as the default runtime
- Source paths (`src/main/java`)
- Output path (`target/build`)
- Code formatting (Google Style)
- Auto-format on save
- Import organization

## 🎯 Current Project Setup

Your project currently uses:
- **Java Version**: Java 22 (JDK 22)
- **Build System**: Manual batch scripts (no Maven/Gradle)
- **Project Structure**: Maven-style directory layout (`src/main/java`)

## 💡 Optional: Consider Adding a Build System

While your batch scripts work fine, you might want to consider:

### Option A: Maven (`pom.xml`)
- Better dependency management
- Standard Java project structure
- IDE integration
- Easy to share with other developers

### Option B: Gradle (`build.gradle`)
- More flexible than Maven
- Better performance
- Modern build tool

Both would provide:
- Better IDE support
- Dependency management
- Standardized build process
- Easier collaboration

## 🔍 Verifying Installation

After installing the extensions:

1. **Check Java Language Support**:
   - Open any `.java` file
   - You should see syntax highlighting, code completion, and error detection

2. **Test Debugging**:
   - Set a breakpoint in `EveMinerAnalyzer.java`
   - Press `F5` to start debugging
   - Should connect to Java debugger

3. **Check Code Quality**:
   - Open a Java file
   - SonarLint should show warnings/errors in the Problems panel

## 📚 Additional Resources

- [VS Code Java Documentation](https://code.visualstudio.com/docs/languages/java)
- [Java Extension Pack](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack)
- [SonarLint Documentation](https://www.sonarlint.org/vscode)

## 🐛 Troubleshooting

### Java Not Detected
1. Make sure Java 22 is installed at `C:\Program Files\Java\jdk-22`
2. Or update `.vscode/settings.json` with your Java path
3. Restart Cursor/VS Code

### Extensions Not Working
1. Reload window: `Ctrl+Shift+P` → "Developer: Reload Window"
2. Check Java is in PATH: Open terminal and run `java -version`
3. Check extension output: `View` → `Output` → Select "Language Support for Java"

### Build Issues
- Your batch scripts should still work independently
- IDE extensions won't interfere with your build process
- You can use both: batch scripts for building, IDE for development


