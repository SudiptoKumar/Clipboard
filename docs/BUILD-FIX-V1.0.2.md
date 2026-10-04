# Build Fix v1.0.2

The GitHub Actions run reached Gradle and failed because the project applied
`org.jetbrains.kotlin.android` together with Android Gradle Plugin 9.4.0.

AGP 9.x provides built-in Kotlin support. Applying the Kotlin Android plugin
again causes:

`Cannot add extension with name 'kotlin', as there is an extension already registered with that name.`

v1.0.2 removes the standalone Kotlin Android plugin from both the root and
module build files. It also removes the deprecated `android.kotlinOptions` DSL.
The project relies on AGP's built-in Kotlin support and Java 17 compile options.
