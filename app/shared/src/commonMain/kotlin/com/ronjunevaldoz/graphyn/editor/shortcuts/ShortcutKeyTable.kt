package com.ronjunevaldoz.graphyn.editor.shortcuts

import androidx.compose.ui.input.key.Key

object ShortcutKeyTable {
    private val keyMap: Map<Key, String> = mapOf(
        Key.A to "A", Key.B to "B", Key.C to "C", Key.D to "D", Key.E to "E",
        Key.F to "F", Key.G to "G", Key.H to "H", Key.I to "I", Key.J to "J",
        Key.K to "K", Key.L to "L", Key.M to "M", Key.N to "N", Key.O to "O",
        Key.P to "P", Key.Q to "Q", Key.R to "R", Key.S to "S", Key.T to "T",
        Key.U to "U", Key.V to "V", Key.W to "W", Key.X to "X", Key.Y to "Y",
        Key.Z to "Z",
        Key.Zero to "0", Key.One to "1", Key.Two to "2", Key.Three to "3", Key.Four to "4",
        Key.Five to "5", Key.Six to "6", Key.Seven to "7", Key.Eight to "8", Key.Nine to "9",
        Key.F1 to "F1", Key.F2 to "F2", Key.F3 to "F3", Key.F4 to "F4", Key.F5 to "F5",
        Key.F6 to "F6", Key.F7 to "F7", Key.F8 to "F8", Key.F9 to "F9", Key.F10 to "F10",
        Key.F11 to "F11", Key.F12 to "F12",
    )

    private val nameToKey: Map<String, Key> = keyMap.entries.associate { (k, v) -> v to k }

    fun keyToName(key: Key): String? = keyMap[key]

    fun keyFromName(name: String): Key? = nameToKey[name]

    fun supportedKeyNames(): List<String> = nameToKey.keys.sorted()
}
