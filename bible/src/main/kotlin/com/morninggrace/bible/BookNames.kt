package com.morninggrace.bible

import com.morninggrace.bible.model.BiblePassage

object BookNames {
    val ZH: Map<Int, String> = mapOf(
        1 to "创世记",       2 to "出埃及记",     3 to "利未记",       4 to "民数记",       5 to "申命记",
        6 to "约书亚记",     7 to "士师记",       8 to "路得记",       9 to "撒母耳记上",   10 to "撒母耳记下",
        11 to "列王纪上",    12 to "列王纪下",    13 to "历代志上",    14 to "历代志下",    15 to "以斯拉记",
        16 to "尼希米记",    17 to "以斯帖记",    18 to "约伯记",      19 to "诗篇",        20 to "箴言",
        21 to "传道书",      22 to "雅歌",        23 to "以赛亚书",    24 to "耶利米书",    25 to "耶利米哀歌",
        26 to "以西结书",    27 to "但以理书",    28 to "何西阿书",    29 to "约珥书",      30 to "阿摩司书",
        31 to "俄巴底亚书",  32 to "约拿书",      33 to "弥迦书",      34 to "那鸿书",      35 to "哈巴谷书",
        36 to "西番雅书",    37 to "哈该书",      38 to "撒迦利亚书",  39 to "玛拉基书",
        40 to "马太福音",    41 to "马可福音",    42 to "路加福音",    43 to "约翰福音",    44 to "使徒行传",
        45 to "罗马书",      46 to "哥林多前书",  47 to "哥林多后书",  48 to "加拉太书",    49 to "以弗所书",
        50 to "腓立比书",    51 to "歌罗西书",    52 to "帖撒罗尼迦前书", 53 to "帖撒罗尼迦后书", 54 to "提摩太前书",
        55 to "提摩太后书",  56 to "提多书",      57 to "腓利门书",    58 to "希伯来书",    59 to "雅各书",
        60 to "彼得前书",    61 to "彼得后书",    62 to "约翰一书",    63 to "约翰二书",    64 to "约翰三书",
        65 to "犹大书",      66 to "启示录"
    )
}

/** Returns a speech-friendly title, e.g. "马太福音第一章". */
fun BiblePassage.toChineseTitle(): String {
    val bookName = BookNames.ZH[book] ?: "第${book}卷"
    return "${bookName}第${chapterToZh(chapter)}章"
}

private fun chapterToZh(n: Int): String {
    val ones = arrayOf("", "一", "二", "三", "四", "五", "六", "七", "八", "九")
    return when {
        n < 10  -> ones[n]
        n == 10 -> "十"
        n < 20  -> "十${ones[n % 10]}"
        n < 100 -> "${ones[n / 10]}十${if (n % 10 > 0) ones[n % 10] else ""}"
        else    -> "一百${if (n % 100 > 0) chapterToZh(n % 100) else ""}"
    }
}
