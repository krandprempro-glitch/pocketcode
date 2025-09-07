package com.termux.app.configuration.models

data class CommandTemplate(
    val id: String,
    val name: String,
    val languageType: LanguageType,
    val commandPattern: String,
    val description: String,
    val isBuiltIn: Boolean = true,
    val defaultParams: Map<String, String> = emptyMap()
) {
    
    companion object {
        val BUILTIN_TEMPLATES = arrayOf(
            CommandTemplate(
                "nodejs_npm_dev",
                "npm run dev",
                LanguageType.NODEJS,
                "npm run dev",
                "Node.js npm开发服务器"
            ),
            CommandTemplate(
                "nodejs_yarn_dev",
                "yarn dev",
                LanguageType.NODEJS,
                "yarn dev",
                "Node.js yarn开发服务器"
            ),
            CommandTemplate(
                "nodejs_pnpm_dev",
                "pnpm dev",
                LanguageType.NODEJS,
                "pnpm dev",
                "Node.js pnpm开发服务器"
            ),
            CommandTemplate(
                "python_flask",
                "python app.py",
                LanguageType.PYTHON,
                "python app.py",
                "Python Flask应用"
            ),
            CommandTemplate(
                "python_django",
                "python manage.py runserver",
                LanguageType.PYTHON,
                "python manage.py runserver",
                "Django开发服务器"
            ),
            CommandTemplate(
                "java_jar",
                "java -jar app.jar",
                LanguageType.JAVA,
                "java -jar app.jar",
                "Java JAR文件执行"
            ),
            CommandTemplate(
                "java_spring_boot",
                "mvn spring-boot:run",
                LanguageType.JAVA,
                "mvn spring-boot:run",
                "Spring Boot Maven启动"
            ),
            CommandTemplate(
                "go_run",
                "go run main.go",
                LanguageType.GO,
                "go run main.go",
                "Go程序直接运行"
            ),
            CommandTemplate(
                "go_build_run",
                "go build && ./app",
                LanguageType.GO,
                "go build && ./app",
                "Go程序编译后运行"
            )
        )
        
        fun getTemplatesByLanguage(language: LanguageType): List<CommandTemplate> {
            return BUILTIN_TEMPLATES.filter { it.languageType == language }
        }
        
        fun getTemplateById(id: String): CommandTemplate? {
            return BUILTIN_TEMPLATES.find { it.id == id }
        }
    }
}