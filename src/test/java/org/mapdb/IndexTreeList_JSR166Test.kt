package org.mapdb

import org.mapdb.jsr166Tests.CopyOnWriteArrayListTest
import java.util.*

class IndexTreeList_JSR166Test:CopyOnWriteArrayListTest(){

    override fun emptyArray():MutableList<Int?> {
        val store = StoreDirect.make();
        val index = IndexTreeLongLongMap.make(store)
        val list = IndexTreeList(store, index, Serializer.INTEGER, true, store.put(0L, Serializer.LONG_PACKED))
        return list
    }

}