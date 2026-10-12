package br.com.lote.core

import br.com.lote.ui.theme.Adaptive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The rules that decide what a lot looks like, none of which may drift between the two apps. */
class LotDisplayTest {

    private val day = 86_400L

    // ---- kind and urgency ----

    @Test
    fun `only AFTER_MARKET is a direct sale`() {
        assertEquals(LotKind.DirectSale, kindOf("AFTER_MARKET"))
        assertEquals(LotKind.Auction, kindOf("LEILAO"))
        assertEquals(LotKind.Auction, kindOf(null))
        assertEquals(LotKind.Auction, kindOf("QUALQUER"))
    }

    @Test
    fun `urgency of an auction`() {
        val auction = LotKind.Auction
        assertEquals(Urgency.Hot, urgencyOf(auction, 1, hasDeadline = true))
        assertEquals(Urgency.Hot, urgencyOf(auction, day, hasDeadline = true))
        assertEquals(Urgency.Normal, urgencyOf(auction, day + 1, hasDeadline = true))
        assertEquals(Urgency.Ended, urgencyOf(auction, 0, hasDeadline = true))
        assertEquals(Urgency.None, urgencyOf(auction, -1, hasDeadline = false))
    }

    @Test
    fun `a deadline that passed is an ended lot, a lot with no deadline never ends`() {
        assertEquals(Urgency.Ended, urgencyOf(LotKind.Auction, -5, hasDeadline = true))
        assertEquals(Urgency.None, urgencyOf(LotKind.Auction, 0, hasDeadline = false))
    }

    @Test
    fun `a direct sale is never hot`() {
        val direct = LotKind.DirectSale
        assertEquals(Urgency.Normal, urgencyOf(direct, 60, hasDeadline = true))
        assertEquals(Urgency.Normal, urgencyOf(direct, day, hasDeadline = true))
        assertEquals(Urgency.Normal, urgencyOf(direct, 35 * day, hasDeadline = true))
        assertEquals(Urgency.Ended, urgencyOf(direct, 0, hasDeadline = true))
        assertEquals(Urgency.None, urgencyOf(direct, -1, hasDeadline = false))
    }

    // ---- price labels ----

    @Test
    fun `the label above a price says what the number is`() {
        assertEquals("Lance atual", priceLabel(LotKind.Auction, Urgency.Normal, 3))
        assertEquals("Lance atual", priceLabel(LotKind.Auction, Urgency.Hot, 1))
        assertEquals("Lance inicial", priceLabel(LotKind.Auction, Urgency.Normal, 0))
        assertEquals("Lance inicial", priceLabel(LotKind.Auction, Urgency.None, 0))
        assertEquals("Lance final", priceLabel(LotKind.Auction, Urgency.Ended, 7))
        assertEquals("Lance final", priceLabel(LotKind.Auction, Urgency.Ended, 0))
        assertEquals("Preço", priceLabel(LotKind.DirectSale, Urgency.Normal, 0))
        assertEquals("Preço", priceLabel(LotKind.DirectSale, Urgency.Ended, 0))
    }

    @Test
    fun `the sticky caption adds the time only to a running auction`() {
        val running = 4 * day + 15 * 3_600
        assertEquals("lance atual · 4d 15h", stickyCaption(LotKind.Auction, Urgency.Normal, 13, running))
        assertEquals("lance inicial · 4d 15h", stickyCaption(LotKind.Auction, Urgency.Normal, 0, running))
        assertEquals("lance final", stickyCaption(LotKind.Auction, Urgency.Ended, 7, 0))
        assertEquals("lance inicial", stickyCaption(LotKind.Auction, Urgency.None, 0, -1))
        assertEquals("preço", stickyCaption(LotKind.DirectSale, Urgency.Normal, 0, running))
    }

    // ---- ficha técnica ----

    @Test
    fun `specs follow the fixed order, then the rest alphabetically`() {
        val specs = linkedMapOf(
            "Peso" to "350",
            "Cor" to "PRATA",
            "Número do Contribuinte" to "1",
            "Ano Fabricação" to "2014",
            "Área Total" to "114",
            "Marca" to "CHEVROLET",
            "Ano Modelo" to "2015",
            "Sexo" to "macho",
            "Modelo" to "ONIX",
            "Valor Tabela FIPE" to "43230",
            "Área Comum" to "37",
            "Registro" to "CSLC 1566",
        )
        assertEquals(
            listOf(
                "Marca", "Modelo", "Ano Modelo", "Ano Fabricação", "Cor", "Valor Tabela FIPE",
                // alphabetical, pt-BR: the accent does not push "Área" past "Número"
                "Área Comum", "Área Total", "Número do Contribuinte", "Peso", "Registro", "Sexo",
            ),
            specOrder(specs).map { it.first },
        )
        assertEquals("CHEVROLET", specOrder(specs).first().second)
    }

    @Test
    fun `specs of every position of the fixed list keep their rank`() {
        val keys = listOf(
            "Marca", "Modelo", "Ano Modelo", "Ano Fabricação", "Cor", "Placa", "Combustível",
            "Câmbio", "Quilometragem", "Valor Tabela FIPE", "Deságio", "Área do Terreno",
            "Área Construída", "Quartos", "Banheiros", "Vagas", "Endereço", "Número da Matrícula",
            "Subtipo", "Condições de Pagamento",
        )
        val shuffled = keys.reversed().associateWith { "x" }
        assertEquals(keys, specOrder(shuffled).map { it.first })
        assertEquals(emptyList<Pair<String, String>>(), specOrder(emptyMap()))
    }

    // ---- purchase conditions ----

    @Test
    fun `purchase conditions are split on the pipe, one per line`() {
        assertEquals(
            listOf("Comissão do leilão: 5%", "Pagamento à vista"),
            purchaseConditions("Comissão do leilão: 5% | Pagamento à vista"),
        )
        assertEquals(listOf("Comissão do leilão: 5%"), purchaseConditions("Comissão do leilão: 5%"))
        assertEquals(emptyList<String>(), purchaseConditions(null))
        assertEquals(emptyList<String>(), purchaseConditions(" | "))
    }

    @Test
    fun `the commission is read out of the conditions`() {
        assertEquals("Comissão do leilão" to "5%", commissionOf(listOf("Comissão do leilão: 5%")))
        assertEquals(
            "Comissão do leilão" to "5%",
            commissionOf(listOf("Entrada de 20%", "Comissão do leilão: 5%")),
        )
        assertEquals("Comissão" to "Comissão de 5%", commissionOf(listOf("Comissão de 5%")))
        assertNull(commissionOf(listOf("Pagamento à vista")))
        assertNull(commissionOf(emptyList()))
    }

    // ---- categories ----

    @Test
    fun `categories read short, after the API's stray spaces are trimmed`() {
        assertEquals("Indústria", categoryShortLabel("Industrial, Máquinas & Equipamentos"))
        assertEquals("Sucatas", categoryShortLabel("Sucatas , Materiais & Resíduos"))
        assertEquals("Joias e relógios", categoryShortLabel("Bolsas, Canetas, Joias e Relógios"))
        assertEquals("Partes e Peças", categoryShortLabel("Partes e Peças "))
        assertEquals("Carros & Motos", categoryShortLabel("Carros & Motos"))
        assertEquals("Imóveis", categoryShortLabel("Imóveis"))
        assertEquals("Outra coisa", categoryShortLabel("  Outra coisa "))
    }

    // ---- grid ----

    @Test
    fun `grid columns follow the window and the reader's text size`() {
        assertEquals(2, Adaptive.gridColumns(411f, 1.0f))
        assertEquals(2, Adaptive.gridColumns(360f, 1.15f))
        assertEquals(1, Adaptive.gridColumns(411f, 1.3f))
        assertEquals(1, Adaptive.gridColumns(411f, 2.0f))
        assertEquals(3, Adaptive.gridColumns(600f, 1.0f))
        assertEquals(3, Adaptive.gridColumns(839f, 1.0f))
        assertEquals(4, Adaptive.gridColumns(840f, 1.0f))
        assertEquals(4, Adaptive.gridColumns(914f, 1.0f))
        assertEquals(2, Adaptive.gridColumns(700f, 1.5f))
        assertEquals(2, Adaptive.gridColumns(914f, 2.0f))
    }
}
