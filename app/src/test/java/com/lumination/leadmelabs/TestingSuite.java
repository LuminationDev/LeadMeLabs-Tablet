package com.lumination.leadmelabs;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Run all UNIT test classes, one after another.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        ModelUnitTest.class,
        FragmentUnitTest.class,
        ViewModelUnitTest.class})
public class TestingSuite {
}
