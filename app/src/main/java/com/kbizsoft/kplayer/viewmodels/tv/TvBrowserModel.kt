package com.kbizsoft.KPlayer.viewmodels.tv

import com.kbizsoft.resources.util.HeaderProvider

interface TvBrowserModel<T> {
    fun isEmpty() : Boolean
    var currentItem: T?
    var nbColumns: Int
    val provider: HeaderProvider
}