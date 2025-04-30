# Manager

## Descripción
Sistema backend desarrollado en Java para la gestión de investigadores y documentación académica. Este proyecto proporciona una API REST para manejar diferentes aspectos relacionados con la investigación académica.

## Estructura del Proyecto
El proyecto está organizado en los siguientes componentes principales:

### Controladores
- `AgentDocumentationController` - Gestión de documentación de agentes
- `AgentManagerController` - Administración de agentes
- `AgentVideoController` - Control de videos relacionados
- `PresentationController` - Manejo de presentaciones
- `ResearcherController` - Gestión de investigadores

### Configuración
- `JdbcConfig` - Configuración de la base de datos
- `RestTemplateConfig` - Configuración del cliente REST
- `RetryConfig` - Configuración de reintentos

### DTOs (Data Transfer Objects)
- Objetos para transferencia de datos incluyendo:
  - Gestión de agentes
  - Documentación
  - Presentaciones
  - Respuestas paginadas
  - Metadatos
  - Integración con GitHub

## Tecnologías Utilizadas
- Java
- Spring Boot
- Docker
- Maven

## Requisitos
- Java 11 o superior
- Maven
- Docker (opcional)

## Instalación

### Usando Maven

## Características Principales
- Gestión de agentes de investigación
- Manejo de documentación académica
- Control de presentaciones y videos
- Integración con GitHub
- Sistema de paginación para respuestas
- Gestión de metadatos de investigación

## Contribución
Para contribuir al proyecto:
1. Fork del repositorio
2. Crear una rama para tu característica (`git checkout -b feature/AmazingFeature`)
3. Commit de tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abrir un Pull Request

## Licencia
[Especificar la licencia]

## Contacto
[Información de contacto del equipo]

