package com.example.dam_t1_evaluacion_practica.data.local

import com.example.dam_t1_evaluacion_practica.domain.model.ChecklistItem
import com.example.dam_t1_evaluacion_practica.domain.model.CriticalityLevel
import com.example.dam_t1_evaluacion_practica.domain.model.Normativ
import com.example.dam_t1_evaluacion_practica.domain.model.NormativesCatalog

object SampleData {
    val normEpp: Normativ = NormativesCatalog.NOM_017_STPS_2008
    val normMachinery: Normativ = NormativesCatalog.NOM_004_STPS_1999
    val normEnvironment: Normativ = NormativesCatalog.NOM_001_STPS_2008

    val eppItems = listOf(
        ChecklistItem(
            id = "epp_1",
            description = "Casco de seguridad: ausencia de fisuras, caducidad del tafilete y barbiquejo ajustado",
            normativ = NormativesCatalog.NOM_017_STPS_2008,
            criticalityLevel = CriticalityLevel.HIGH,
            requiresPhotoEvidence = true,
            secondaryNorms = listOf(NormativesCatalog.NOM_115_STPS_2009)
        ),
        ChecklistItem(
            id = "epp_2",
            description = "Calzado de protección: suela antideslizante en buen estado y casquillo intacto",
            normativ = NormativesCatalog.NOM_017_STPS_2008,
            criticalityLevel = CriticalityLevel.HIGH,
            requiresPhotoEvidence = true,
            secondaryNorms = listOf(NormativesCatalog.NOM_113_STPS_2009)
        ),
        ChecklistItem(
            id = "epp_3",
            description = "Arnés y líneas de vida: estado de las costuras, ausencia de desgaste por fricción o químicos y verificación de la fecha de última certificación",
            normativ = NormativesCatalog.NOM_009_STPS_2011,
            criticalityLevel = CriticalityLevel.HIGH,
            requiresPhotoEvidence = true,
            secondaryNorms = listOf(NormativesCatalog.NOM_017_STPS_2008)
        ),
        ChecklistItem(
            id = "epp_4",
            description = "Protección ocular: disponibilidad y uso efectivo según la zona de trabajo",
            normativ = NormativesCatalog.NOM_017_STPS_2008,
            criticalityLevel = CriticalityLevel.MEDIUM,
            requiresPhotoEvidence = false
        ),
        ChecklistItem(
            id = "epp_5",
            description = "Protección auditiva: disponibilidad y uso efectivo según el nivel de ruido de la zona de trabajo",
            normativ = NormativesCatalog.NOM_017_STPS_2008,
            criticalityLevel = CriticalityLevel.MEDIUM,
            requiresPhotoEvidence = false
        )
    )

    val machineryItems = listOf(
        ChecklistItem(
            id = "mach_1",
            description = "Extintores de incendios: presión en manómetro (zona verde), sello de seguridad intacto y fecha de recarga vigente",
            normativ = NormativesCatalog.NOM_002_STPS_2010,
            criticalityLevel = CriticalityLevel.HIGH,
            requiresPhotoEvidence = true,
            secondaryNorms = listOf(NormativesCatalog.NOM_154_SCFI_2005)
        ),
        ChecklistItem(
            id = "mach_2",
            description = "Sistemas eléctricos y tableros: candado de bloqueo/etiquetado (LOTO) colocado y cables sin partes expuestas",
            normativ = NormativesCatalog.NOM_029_STPS_2011,
            criticalityLevel = CriticalityLevel.HIGH,
            requiresPhotoEvidence = true,
            secondaryNorms = listOf(NormativesCatalog.NOM_004_STPS_1999)
        ),
        ChecklistItem(
            id = "mach_3",
            description = "Sistemas eléctricos y tableros: conexión a tierra física verificada",
            normativ = NormativesCatalog.NOM_029_STPS_2011,
            criticalityLevel = CriticalityLevel.HIGH,
            requiresPhotoEvidence = true,
            secondaryNorms = listOf(NormativesCatalog.NOM_001_SEDE_2012)
        ),
        ChecklistItem(
            id = "mach_4",
            description = "Andamios y plataformas: tarjeta de autorización verde ('Apto para uso') vigente",
            normativ = NormativesCatalog.NOM_009_STPS_2011,
            criticalityLevel = CriticalityLevel.HIGH,
            requiresPhotoEvidence = true
        ),
        ChecklistItem(
            id = "mach_5",
            description = "Andamios y plataformas: barandales instalados y nivelación correcta",
            normativ = NormativesCatalog.NOM_009_STPS_2011,
            criticalityLevel = CriticalityLevel.MEDIUM,
            requiresPhotoEvidence = false
        )
    )

    val environmentItems = listOf(
        ChecklistItem(
            id = "env_1",
            description = "Vías de evacuación y salidas de emergencia: despejadas y señalizadas",
            normativ = NormativesCatalog.NOM_002_STPS_2010,
            criticalityLevel = CriticalityLevel.HIGH,
            requiresPhotoEvidence = true,
            secondaryNorms = listOf(NormativesCatalog.NOM_026_STPS_2008, NormativesCatalog.NOM_001_STPS_2008)
        ),
        ChecklistItem(
            id = "env_2",
            description = "Vías de evacuación y salidas de emergencia: iluminación de respaldo funcional",
            normativ = NormativesCatalog.NOM_001_STPS_2008,
            criticalityLevel = CriticalityLevel.HIGH,
            requiresPhotoEvidence = false,
            secondaryNorms = listOf(NormativesCatalog.NOM_002_STPS_2010)
        ),
        ChecklistItem(
            id = "env_3",
            description = "Orden y limpieza: ausencia de derrames de aceites o sustancias químicas",
            normativ = NormativesCatalog.NOM_005_STPS_1998,
            criticalityLevel = CriticalityLevel.MEDIUM,
            requiresPhotoEvidence = false,
            secondaryNorms = listOf(NormativesCatalog.NOM_001_STPS_2008)
        ),
        ChecklistItem(
            id = "env_4",
            description = "Orden y limpieza: acopio adecuado de residuos",
            normativ = NormativesCatalog.NOM_001_STPS_2008,
            criticalityLevel = CriticalityLevel.MEDIUM,
            requiresPhotoEvidence = false,
            secondaryNorms = listOf(NormativesCatalog.NOM_006_STPS_2014)
        ),
        ChecklistItem(
            id = "env_5",
            description = "Orden y limpieza: pasillos y áreas de trabajo libres de obstáculos",
            normativ = NormativesCatalog.NOM_001_STPS_2008,
            criticalityLevel = CriticalityLevel.LOW,
            requiresPhotoEvidence = false,
            secondaryNorms = listOf(NormativesCatalog.NOM_006_STPS_2014)
        )
    )
}
