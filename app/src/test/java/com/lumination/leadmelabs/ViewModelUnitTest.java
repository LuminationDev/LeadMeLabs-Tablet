package com.lumination.leadmelabs;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.lumination.leadmelabs.ui.nuc.NucViewModel;

import org.junit.Rule;
import org.junit.rules.TestRule;

public class ViewModelUnitTest {
    //Variables
    private NucViewModel viewModel;

    //Testing area
    @Rule
    public TestRule rule = new InstantTaskExecutorRule();
}
