/*
 *  Copyright 2022 CNM Ingenuity, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package edu.cnm.deepdive.dla.controller

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import edu.cnm.deepdive.dla.R
import edu.cnm.deepdive.dla.databinding.ActivityMainBinding
import edu.cnm.deepdive.dla.view.LatticeView
import edu.cnm.deepdive.dla.viewmodel.MainViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private var running = false
        set(value) {
            field = value
            if (value) {
                if (startPending) {
                    startPending = false
                } else {
                    viewModel.accumulate()
                }
            }
            invalidateOptionsMenu()
        }
    private var startPending = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        running = false
        startPending = false
        setupViewModel()
        setupUI()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.main_options, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        with(menu) {
            super.onPrepareOptionsMenu(menu)
            findItem(R.id.reset).isVisible = !running
            findItem(R.id.play).isVisible = !running
            findItem(R.id.pause).isVisible = running
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var handled = true
        val id = item.itemId
        if (id == R.id.reset) {
            binding.lattice.clear()
            viewModel.clear()
        } else if (id == R.id.play) {
            startPending = true
            viewModel.resume()
        } else if (id == R.id.pause) {
            viewModel.pause()
        } else {
            handled = super.onOptionsItemSelected(item)
        }
        return handled
    }

    private fun setupViewModel() {
        val owner = this
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
            .apply {
                lifecycle.addObserver(this)
                running.observe(owner) { owner.running = it }
                throwable.observe(owner) { throwable: Throwable? -> handleThrowable(throwable) }
            }
    }

    private fun setupUI() {
        val owner = this
        binding = DataBindingUtil
            .setContentView<ActivityMainBinding?>(this, R.layout.activity_main)
            .apply {
                viewModel = owner.viewModel
                lifecycleOwner = owner
                lattice.onSeedListener = { _: LatticeView, x: Int, y: Int -> viewModel?.set(x, y) }
            }
    }


    private fun handleThrowable(throwable: Throwable?) {
        throwable?.let {
            Toast
                .makeText(this, it.message, Toast.LENGTH_LONG)
                .show()
        }
    }
}