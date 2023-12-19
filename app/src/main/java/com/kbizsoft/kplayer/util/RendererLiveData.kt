package com.kbizsoft.KPlayer.util

import androidx.lifecycle.MutableLiveData
import com.kbizsoft.libkplayer.RendererItem


class RendererLiveData : MutableLiveData<RendererItem>() {

    override fun setValue(value: RendererItem?) {
        getValue()?.release()
        value?.retain()
        super.setValue(value)
    }
}