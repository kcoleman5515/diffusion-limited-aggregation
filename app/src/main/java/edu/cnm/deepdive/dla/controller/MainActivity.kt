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
package edu.cnm.deepdive.dla.controller;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import edu.cnm.deepdive.dla.R;
import edu.cnm.deepdive.dla.databinding.ActivityMainBinding;
import edu.cnm.deepdive.dla.viewmodel.MainViewModel;

public class MainActivity extends AppCompatActivity {

  private ActivityMainBinding binding;
  private MainViewModel viewModel;
  private boolean running;
  private boolean startPending;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    running = false;
    startPending = false;
    setupViewModel();
    setupUI();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    getMenuInflater().inflate(R.menu.main_options, menu);
    return true;
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);
    menu.findItem(R.id.reset).setVisible(!running);
    menu.findItem(R.id.play).setVisible(!running);
    menu.findItem(R.id.pause).setVisible(running);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    boolean handled = true;
    int id = item.getItemId();
    if (id == R.id.reset) {
      binding.lattice.clear();
      viewModel.clear();
    } else if (id == R.id.play) {
      startPending = true;
      viewModel.setRunning(true);
    } else if (id == R.id.pause) {
      viewModel.setRunning(false);
    } else {
      handled = super.onOptionsItemSelected(item);
    }
    return handled;
  }

  private void setupViewModel() {
    viewModel = new ViewModelProvider(this).get(MainViewModel.class);
    getLifecycle().addObserver(viewModel);
    viewModel
        .getRunning()
        .observe(this, this::setRunning);
    viewModel
        .getThrowable()
        .observe(this, this::handleThrowable);
  }

  private void setupUI() {
    binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
    binding.setViewModel(viewModel);
    binding.setLifecycleOwner(this);
    binding.lattice.setOnSeedListener((v, x, y) -> viewModel.set(x, y));
  }

  private void setRunning(boolean running) {
    this.running = running;
    if (running) {
      if (startPending) {
        startPending = false;
      } else {
        viewModel.accumulate();
      }
    }
    invalidateOptionsMenu();
  }

  private void handleThrowable(Throwable throwable) {
    if (throwable != null) {
      Toast.makeText(this, throwable.getMessage(), Toast.LENGTH_LONG).show();
    }
  }

}