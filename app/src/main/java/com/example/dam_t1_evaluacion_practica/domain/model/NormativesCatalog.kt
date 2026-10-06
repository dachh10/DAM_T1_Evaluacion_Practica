package com.example.dam_t1_evaluacion_practica.domain.model

/**
 * Official Mexican Standards (NOM-STPS and related) catalog for operational safety inspections.
 * Contains direct links to official repositories (Platiica / DOF) and key regulatory criteria.
 */
object NormativesCatalog {

    // --- EPP & Protección Personal ---
    val NOM_017_STPS_2008 = Normativ(
        code = "NOM-017-STPS-2008",
        title = "Equipo de Protección Personal",
        description = "Selección, uso y manejo del equipo de protección personal en los centros de trabajo.",
        officialUrl = "https://platiica.economia.gob.mx/normalizacion/?p=35234",
        keyRequirement = "Obligatoriedad de dotar y verificar el uso adecuado del EPP certificado acorde al análisis de riesgos del puesto.",
        issuingAuthority = "STPS"
    )

    val NOM_113_STPS_2009 = Normativ(
        code = "NOM-113-STPS-2009",
        title = "Calzado de Protección",
        description = "Clasificación, especificaciones y métodos de prueba para calzado de protección.",
        officialUrl = "https://platiica.economia.gob.mx/normalizacion/nom-113-stps-2009/",
        keyRequirement = "Casquillo de protección intacto, suela antideslizante con resistencia a la penetración y propiedades dieléctricas si aplica.",
        issuingAuthority = "STPS"
    )

    val NOM_115_STPS_2009 = Normativ(
        code = "NOM-115-STPS-2009",
        title = "Cascos de Protección",
        description = "Clasificación, especificaciones y métodos de prueba para cascos de protección.",
        officialUrl = "https://platiica.economia.gob.mx/normalizacion/nom-115-stps-2009/",
        keyRequirement = "Concha sin fisuras ni impactos previos, tafilete/suspensión vigente y barbiquejo de sujeción ajustado.",
        issuingAuthority = "STPS"
    )

    // --- Trabajos en Altura & Andamios ---
    val NOM_009_STPS_2011 = Normativ(
        code = "NOM-009-STPS-2011",
        title = "Trabajos en Altura",
        description = "Condiciones de seguridad para realizar trabajos en altura.",
        officialUrl = "https://platiica.economia.gob.mx/normalizacion/nom-009-stps-2011/",
        keyRequirement = "Uso mandatorio de arnés de cuerpo entero con línea de vida (>1.80m), certificación vigente, andamios con tarjeta verde y barandales a 90cm/110cm.",
        issuingAuthority = "STPS"
    )

    // --- Maquinaria & Equipo ---
    val NOM_004_STPS_1999 = Normativ(
        code = "NOM-004-STPS-1999",
        title = "Maquinaria y Equipo",
        description = "Sistemas de protección y dispositivos de seguridad en la maquinaria y equipo que se utilice en los centros de trabajo.",
        officialUrl = "https://platiica.economia.gob.mx/normalizacion/nom-004-stps-1999/",
        keyRequirement = "Resguardos de partes móviles, paros de emergencia operables y dispositivos de seguridad activos.",
        issuingAuthority = "STPS"
    )

    // --- Instalaciones Eléctricas & LOTO ---
    val NOM_029_STPS_2011 = Normativ(
        code = "NOM-029-STPS-2011",
        title = "Mantenimiento de Instalaciones Eléctricas",
        description = "Mantenimiento de las instalaciones eléctricas en los centros de trabajo - Condiciones de seguridad.",
        officialUrl = "https://platiica.economia.gob.mx/normalizacion/nom-029-stps-2011/",
        keyRequirement = "Aplicación estricta del procedimiento de Bloqueo y Etiquetado (LOTO), ausencia de cableado expuesto y permisos para trabajo con tensión.",
        issuingAuthority = "STPS"
    )

    val NOM_001_SEDE_2012 = Normativ(
        code = "NOM-001-SEDE-2012",
        title = "Instalaciones Eléctricas (Puesta a Tierra)",
        description = "Instalaciones Eléctricas (utilización) - Especificaciones de seguridad y puesta a tierra física.",
        officialUrl = "https://platiica.economia.gob.mx/normalizacion/nom-001-sede-2012/",
        keyRequirement = "Conexión a electrodo de tierra física continua con valor óhmico dentro de los límites permisibles.",
        issuingAuthority = "SENER"
    )

    // --- Prevención de Incendios & Extintores ---
    val NOM_002_STPS_2010 = Normativ(
        code = "NOM-002-STPS-2010",
        title = "Prevención y Protección contra Incendios",
        description = "Condiciones de seguridad - Prevención y protección contra incendios en los centros de trabajo.",
        officialUrl = "https://platiica.economia.gob.mx/normalizacion/NOM-002-STPS-2010",
        keyRequirement = "Extintores ubicados a no más de 1.50m de altura, manómetro en zona operable, salidas de emergencia señalizadas y libres de obstáculos.",
        issuingAuthority = "STPS"
    )

    val NOM_154_SCFI_2005 = Normativ(
        code = "NOM-154-SCFI-2005",
        title = "Mantenimiento y Recarga de Extintores",
        description = "Equipos contra incendio - Extintores - Servicio de mantenimiento y recarga.",
        officialUrl = "https://platiica.economia.gob.mx/normalizacion/nom-154-scfi-2005/",
        keyRequirement = "Collarín de verificación de servicio, holograma/etiqueta de mantenimiento vigente del prestador de servicio acreditado.",
        issuingAuthority = "SE"
    )

    // --- Edificios, Vías de Evacuación & Señalización ---
    val NOM_001_STPS_2008 = Normativ(
        code = "NOM-001-STPS-2008",
        title = "Edificios, Locales e Instalaciones",
        description = "Edificios, locales, instalaciones y áreas en los centros de trabajo - Condiciones de seguridad.",
        officialUrl = "https://platiica.economia.gob.mx/normalizacion/nom-001-stps-2008/",
        keyRequirement = "Vías de circulación y salidas de emergencia expeditas, iluminación de respaldo funcional y pisos sin desniveles peligrosos.",
        issuingAuthority = "STPS"
    )

    val NOM_026_STPS_2008 = Normativ(
        code = "NOM-026-STPS-2008",
        title = "Colores y Señales de Seguridad",
        description = "Colores y señales de seguridad e higiene, e identificación de riesgos por fluidos conducidos en tuberías.",
        officialUrl = "https://platiica.economia.gob.mx/normalizacion/nom-026-stps-2008/",
        keyRequirement = "Señalética fotoluminiscente o iluminada indicando rutas de evacuación, puntos de reunión y ubicación de equipos de emergencia.",
        issuingAuthority = "STPS"
    )

    val NOM_005_STPS_1998 = Normativ(
        code = "NOM-005-STPS-1998",
        title = "Manejo de Sustancias Químicas",
        description = "Condiciones de seguridad e higiene para el manejo, transporte y almacenamiento de sustancias químicas peligrosas.",
        officialUrl = "https://platiica.economia.gob.mx/normalizacion/nom-005-stps-1998/",
        keyRequirement = "Contención de derrames con charolas o diques, hojas de datos de seguridad (HDS) disponibles y kit antiderrame.",
        issuingAuthority = "STPS"
    )

    val NOM_006_STPS_2014 = Normativ(
        code = "NOM-006-STPS-2014",
        title = "Manejo y Almacenamiento de Materiales",
        description = "Manejo y almacenamiento de materiales - Condiciones de seguridad y salud en el trabajo.",
        officialUrl = "https://platiica.economia.gob.mx/normalizacion/nom-006-stps-2014/",
        keyRequirement = "Estiba segura de materiales sin sobrepasar capacidades de carga, orden y delimitación de pasillos.",
        issuingAuthority = "STPS"
    )

    val ALL_NORMATIVES = listOf(
        NOM_017_STPS_2008,
        NOM_113_STPS_2009,
        NOM_115_STPS_2009,
        NOM_009_STPS_2011,
        NOM_004_STPS_1999,
        NOM_029_STPS_2011,
        NOM_001_SEDE_2012,
        NOM_002_STPS_2010,
        NOM_154_SCFI_2005,
        NOM_001_STPS_2008,
        NOM_026_STPS_2008,
        NOM_005_STPS_1998,
        NOM_006_STPS_2014
    )
}
