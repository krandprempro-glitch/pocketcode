package com.termux.app.configuration.models

enum class LanguageType(
    val displayName: String,
    val identifier: String,
    val commonCommands: Array<String>
) {
    NODEJS("Node.js", "node", arrayOf("npm run dev", "yarn dev", "pnpm dev", "npm start")),
    PYTHON("Python", "python", arrayOf("python app.py", "python manage.py runserver", "gunicorn app:app", "flask run")),
    JAVA("Java", "java", arrayOf("java -jar app.jar", "mvn spring-boot:run", "gradle bootRun", "./gradlew bootRun")),
    GO("Go", "go", arrayOf("go run main.go", "go run .", "./app", "go build && ./app")),
    CUSTOM("自定义", "custom", arrayOf());
    
    companion object {
        fun fromIdentifier(identifier: String): LanguageType? {
            return values().find { it.identifier == identifier }
        }
        
        fun fromDisplayName(displayName: String): LanguageType? {
            return values().find { it.displayName == displayName }
        }
    }
}