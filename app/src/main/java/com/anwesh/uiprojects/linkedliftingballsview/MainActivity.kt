package com.anwesh.uiprojects.linkedliftingballsview

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.anwesh.uiprojects.liftingballsview.LiftingBallsView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LiftingBallsView.create(this)
    }
}
