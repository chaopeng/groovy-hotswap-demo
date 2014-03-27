package me.chaopeng.hotswap.handler
import me.chaopeng.hotswap.CMD

/**
 * Handler
 * @author chao
 * @version 1.0 - 2014-03-27
 */

@CMD(1)
void hello(){
    println("run in " + Integer.toHexString(System.identityHashCode(getClass())))
    println("hello")
}

