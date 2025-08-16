/*
 * The MIT License
 *
 * Copyright (c) 2024, Daniel Beck
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package io.jenkins.plugins.peopleview;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Action;
import hudson.model.View;
import java.util.Objects;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.StaplerProxy;

public class AsynchPeopleAction implements Action, StaplerProxy {

    private final View view;
    private final Jenkins jenkins;

    public AsynchPeopleAction(@NonNull View v) {
        this.view = v;
        this.jenkins = null;
    }

    public AsynchPeopleAction(@NonNull Jenkins j) {
        this.view = null;
        this.jenkins = j;
    }

    @Override
    public String getIconFileName() {
        return "symbol-people";
    }

    @Override
    public String getDisplayName() {
        return Messages.People_DisplayName();
    }

    @Override
    public String getUrlName() {
        return "asynchPeople";
    }

    @Override
    public Object getTarget() {
        if (view == null) {
            return new AsynchPeople(Objects.requireNonNull(jenkins));
        }
        return new AsynchPeople(view);
    }
}
