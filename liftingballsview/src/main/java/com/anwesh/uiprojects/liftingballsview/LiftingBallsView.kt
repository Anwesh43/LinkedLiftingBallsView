package com.anwesh.uiprojects.liftingballsview

/**
 * Created by anweshmishra on 20/03/19.
 */

import android.view.View
import android.view.MotionEvent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.app.Activity
import android.content.Context

val nodes : Int = 5
val balls : Int = 3
val scGap : Float = 0.05f
val scDiv : Double = 0.51
val sizeFactor : Float = 2.9f
val strokeFactor : Int = 90
val foreColor : Int = Color.parseColor("#283593")
val backColor : Int = Color.parseColor("#BDBDBD")
val rFactor : Int = 3
val delay : Long = 20

fun Int.inverse() : Float = 1f / this
fun Float.maxScale(i : Int, n : Int) : Float = Math.max(0f, this - i * n.inverse())
fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.inverse(), maxScale(i, n)) * n
fun Float.scaleFactor() : Float = Math.floor(this / scDiv).toFloat()
fun Float.mirrorValue(a : Int, b : Int) : Float = (1 - scaleFactor()) * a.inverse() + scaleFactor() * b.inverse()
fun Float.updateValue(dir : Float, a : Int, b : Int) : Float = mirrorValue(a, b) * dir * scGap

fun Canvas.drawLBNode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    val gap : Float = w / (nodes + 1)
    val size : Float = gap / sizeFactor
    val sc1 : Float = scale.divideScale(0, 2)
    val sc2 : Float = scale.divideScale(1, 2)
    val xGap : Float = (2 * size) / balls
    paint.color = foreColor
    paint.strokeCap = Paint.Cap.ROUND
    paint.strokeWidth = Math.min(w, h) / strokeFactor
    save()
    translate(gap * (i + 1), h / 2)
    rotate(90f * sc2)
    for (j in 0..(balls - 1)) {
        val scj : Float = sc1.divideScale(j, balls)
        val scj1 : Float = scj.divideScale(0, 2)
        val scj2 : Float = scj.divideScale(1, 2)
        val r : Float = xGap / rFactor
        val y : Float = -size * scj1
        save()
        translate(-size + xGap * j,  0f)
        save()
        translate(0f, y)
        drawLine(0f, 0f, xGap, 0f, paint)
        drawCircle(xGap / 2, -r, r * scj2, paint)
        restore()
        drawLine(xGap / 2, 0f, xGap / 2, y, paint)
        restore()
    }
    restore()
}

class LiftingBallsView(ctx : Context) : View(ctx) {

    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas, paint)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var prevScale : Float = 0f, var dir : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += scale.updateValue(dir, balls, 1)
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(delay)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class LBNode(var i : Int, val state : State = State()) {

        private var next : LBNode? = null
        private var prev : LBNode? = null

        init {
            addNeighbor()
        }


        fun addNeighbor() {
            if (i < nodes - 1) {
                next = LBNode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawLBNode(i, state.scale, paint)
            next?.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            state.update {
                cb(i, it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : LBNode {
            var curr : LBNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class LiftingBalls(var i : Int) {

        private val root : LBNode = LBNode(0)
        private var curr : LBNode = root
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            root.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            curr.update {i, scl ->
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(i, scl)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : LiftingBallsView) {

        private val lb : LiftingBalls = LiftingBalls(0)
        private var animator : Animator = Animator(view)

        fun render(canvas : Canvas, paint : Paint) {
            canvas.drawColor(backColor)
            lb.draw(canvas, paint)
            animator.animate {
                lb.update {i, scl ->
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            lb.startUpdating {
                animator.start()
            }
        }
    }

    companion object {

        fun create(activity : Activity) : LiftingBallsView {
            val view : LiftingBallsView = LiftingBallsView(activity)
            activity.setContentView(view)
            return view
        }
    }
}