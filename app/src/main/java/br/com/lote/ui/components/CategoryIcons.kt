package br.com.lote.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Agriculture
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Chair
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.PrecisionManufacturing
import androidx.compose.material.icons.outlined.Recycling
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Sailing
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Warehouse
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The glyph of a category, from the specification's table, compared after `trim()`: the API keeps
 * stray spaces in some names. `forklift` does not exist in material-icons-extended and the
 * warehouse stands in for it. Anything unlisted wears the tag, so a category added later never
 * shows an empty square.
 */
fun categoryIcon(name: String): ImageVector = when (name.trim()) {
    "Imóveis" -> Icons.Outlined.Apartment
    "Carros & Motos" -> Icons.Outlined.DirectionsCar
    "Caminhões & Ônibus" -> Icons.Outlined.LocalShipping
    "Máquinas Pesadas & Agrícolas" -> Icons.Outlined.Agriculture
    "Movimentação & Transporte" -> Icons.Outlined.Warehouse
    "Industrial, Máquinas & Equipamentos" -> Icons.Outlined.PrecisionManufacturing
    "Animais" -> Icons.Outlined.Pets
    "Tecnologia" -> Icons.Outlined.Devices
    "Móveis e Decoração" -> Icons.Outlined.Chair
    "Bolsas, Canetas, Joias e Relógios" -> Icons.Outlined.Watch
    "Sucatas , Materiais & Resíduos" -> Icons.Outlined.Recycling
    "Embarcações & Aeronaves" -> Icons.Outlined.Sailing
    "Eletrodomésticos" -> Icons.Outlined.Kitchen
    "Materiais para Construção Civil" -> Icons.Outlined.Construction
    "Cozinhas e Restaurantes" -> Icons.Outlined.Restaurant
    "Alimentos e Bebidas" -> Icons.Outlined.LocalCafe
    "Oportunidades" -> Icons.Outlined.Sell
    "Artes, Decoração & Colecionismo" -> Icons.Outlined.Palette
    "Partes e Peças" -> Icons.Outlined.Build
    else -> Icons.Outlined.Sell
}
