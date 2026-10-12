package br.com.loyfus.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * The title cleaning is a port, so it is tested the way a port is: the reference's own
 * vectors, then every real title the reference was run over, which must come out
 * identical. If a row disagrees the port is wrong, never the golden file.
 */
class TidyTitleTest {

    private class Vector(val raw: String, val headline: String?, val detail: String?)

    // docs/design-v3/tidy_title.py, VECTORS. A null headline means "already readable:
    // must come back untouched".
    private val vectors = listOf(
        Vector("CHEVROLET CLASSIC LS 2010 2011 CINZA", "Chevrolet Classic LS 2010/2011 Cinza", null),
        Vector("VW GOL GL 2P 1989 1989 CINZA", "VW Gol GL 2P 1989/1989 Cinza", null),
        Vector("RENAULT CLIO PRI 10 16VH 2004 2004 CINZA", "Renault Clio PRI 10 16VH 2004/2004 Cinza", null),
        Vector(
            "CHEVROLET SPIN 18L AT PREMIER, 2024/2025, Placa FINAL 0 (SP), (Ref.: 53137)",
            "Chevrolet Spin 18L AT Premier, 2024/2025, Placa Final 0 (SP)", null,
        ),
        Vector("TOYOTA/COROLLA XEI20FLEX, 2016/2017, AZUL", "Toyota/Corolla XEI20FLEX, 2016/2017, Azul", null),
        Vector(
            "M.BENZ/LK 1313 - Ano/Modelo 1987/1987 - Combustível DIESEL",
            "M.Benz/LK 1313 - Ano/Modelo 1987/1987 - Combustível Diesel", null,
        ),
        Vector("EMPILHADEIRA TOYOTA 8FG25 DIESEL ID: #3614548", "Empilhadeira Toyota 8FG25 Diesel", null),
        Vector("LAVADORA E TANQUINHO (#50MAGO)", "Lavadora e Tanquinho", null),
        Vector(
            "CHEVROLET TRACKER 12T A RS, 2023/2024, Placa FINAL 2 (SP), (Ref.: 53166)",
            "Chevrolet Tracker 12T A RS, 2023/2024, Placa Final 2 (SP)", null,
        ),
        Vector("FIAT PALIO ELX EDX 1.0 MPI 1998 1999 CINZA", "Fiat Palio ELX EDX 1.0 MPI 1998/1999 Cinza", null),
        Vector("NOVILHA NELORE - ATHENA FIV SNL - SNL 1517", "Novilha Nelore - Athena FIV SNL - SNL 1517", null),
        Vector(
            "TRATOR JOHN DEERE 6110E 4X4 ANO 2014 1.185 HORAS",
            "Trator John Deere 6110E 4X4 Ano 2014 1.185 Horas", null,
        ),
        Vector("Máquina de escrever Antiga OLIVETTI LINEA 98", "Máquina de escrever Antiga Olivetti Linea 98", null),
        Vector(
            "121 UNIDADES NOVAS ¿ BOX PARA CAMA, BOX BAÚ, CAMAS AUXILIARES E CABECEIRAS",
            "121 Unidades Novas – Box para Cama, Box Baú, Camas Auxiliares e Cabeceiras", null,
        ),
        Vector(
            "¿¿UM TELEVISOR, MARCA SHARP, 32 POLEGADAS e UM REFRIGERADOR",
            "Um Televisor, Marca Sharp, 32 Polegadas e um Refrigerador", null,
        ),
        Vector(
            "Casa 78m² com Terreno 300m² | Rosário Do Sul-RS | Vila Nova - Financiamento até 420 Meses!",
            "Casa 78m² com Terreno 300m²", "Rosário Do Sul-RS · Vila Nova - Financiamento até 420 Meses!",
        ),
        Vector(
            "Apartamento 114,97m², Jardim Monte Alegre, Itapecerica da Serra/SP",
            "Apartamento 114,97m², Jardim Monte Alegre, Itapecerica da Serra/SP", null,
        ),
        Vector(
            "TORNO MECÂNICO DE PRECISÃO SANCHES COM PLACA UNIVERSAL",
            "Torno Mecânico de Precisão Sanches com Placa Universal", null,
        ),
        Vector("NELORE MOCHA - MAAB EMPINADO - MABY 5297", "Nelore Mocha - Maab Empinado - Maby 5297", null),
        Vector("Geladeira, freezer, ar condicionado, máquina de lavar roupas", null, null),
    )

    @Test
    fun `the reference vectors`() {
        assertEquals(20, vectors.size)
        for (v in vectors) {
            val got = tidyTitle(v.raw)
            assertEquals("headline of: ${v.raw}", v.headline ?: v.raw, got.headline)
            assertEquals("detail of: ${v.raw}", v.detail, got.detail)
        }
    }

    @Test
    fun `the golden file of real catalogue titles matches the reference exactly`() {
        val file = goldenFile()
        val rows = file.readLines(Charsets.UTF_8).filter { it.isNotEmpty() }
        assertEquals("rows in ${file.path}", 1566, rows.size)

        val failures = mutableListOf<String>()
        for ((number, line) in rows.withIndex()) {
            val columns = line.split('\t')
            assertEquals("columns on row ${number + 1}: $line", 3, columns.size)
            val (raw, headline, detail) = columns
            val got = tidyTitle(raw)
            val wantDetail = detail.ifEmpty { null }
            if (got.headline != headline || got.detail != wantDetail) {
                failures += "row ${number + 1}: $raw\n   want: [$headline] [$wantDetail]\n   got : [${got.headline}] [${got.detail}]"
            }
        }
        if (failures.isNotEmpty()) {
            fail("${failures.size} of ${rows.size} golden rows differ:\n" + failures.take(15).joinToString("\n"))
        }
    }

    /**
     * The golden table is owned by the backend repo, which sits beside this one. Gradle runs
     * unit tests from the module directory, so walk up looking in each root and its backend
     * sibling. A missing file fails the build: a skipped golden test would let the port drift
     * without anyone noticing.
     */
    private fun goldenFile(): File {
        val relative = "docs/design-v3/tidy_title_golden.tsv"
        val start = requireNotNull(System.getProperty("user.dir")) { "user.dir is not set" }
        var dir: File? = File(start).absoluteFile
        while (dir != null) {
            for (candidate in listOf(File(dir, relative), File(dir, "backend/$relative"))) {
                if (candidate.isFile) return candidate
            }
            dir = dir.parentFile
        }
        fail(
            "Golden file not found: looked for $relative in $start, in every parent directory, " +
                "and in their backend/ siblings. The title test cannot pass without it."
        )
        error("unreachable")
    }

    // ---- what the title cleaning is allowed to touch ----

    @Test
    fun `a title with no pipe has no detail and the pipe splits at the first one only`() {
        assertNull(tidyTitle("TERRENO 300M2").detail)
        val t = tidyTitle("Lote 371m² | DESOCUPADO | Bragança Paulista-SP | Condomínio Villa Real")
        assertEquals("Lote 371m²", t.headline)
        assertEquals("Desocupado · Bragança Paulista-SP · Condomínio Villa Real", t.detail)
    }

    @Test
    fun `an empty detail is no detail`() {
        assertNull(tidyTitle("CASA 100M2 | ").detail)
    }

    @Test
    fun `a code of the auctioneer disappears and a state code survives`() {
        assertEquals("Empilhadeira Toyota", tidyTitle("EMPILHADEIRA TOYOTA ID #3753852/EL").headline)
        assertTrue(tidyTitle("FIAT UNO (SP)").headline.endsWith("(SP)"))
        assertTrue(tidyTitle("CASA CENTRO - RS").headline.endsWith("RS"))
    }

    // ---- place ----

    @Test
    fun `place vector of the specification`() {
        assertEquals(
            "Rosário Do Sul-RS",
            place("Rosário Do Sul-RS · Vila Nova - Financiamento até 420 Meses!"),
        )
    }

    @Test
    fun `place finds the first piece that is a city and a state code`() {
        assertEquals("Bragança Paulista-SP", place("Desocupado · Bragança Paulista-SP · Condomínio Villa Real"))
        assertEquals("Ituiutaba-MG", place("Desocupada · Ituiutaba-MG · Lagoa Azul II"))
        assertEquals("Salvador-BA", place("Salvador-BA"))
        assertNull(place("Centro - Amplo Terreno em Excelente Localização"))
        assertNull(place("Próx. à BR-101 e Shopping"))
        assertNull(place(null))
        assertNull(place(""))
    }
}
