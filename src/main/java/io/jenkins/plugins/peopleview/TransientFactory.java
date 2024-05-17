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

import hudson.Extension;
import hudson.model.Action;
import hudson.model.AllView;
import hudson.model.TransientViewActionFactory;
import hudson.model.View;
import java.util.List;
import jenkins.model.Jenkins;

@Extension
public class TransientFactory extends TransientViewActionFactory {

    @Override
    public List<Action> createFor(View v) {
        final Jenkins j = Jenkins.get();
        if (j.equals(v.getOwner()) && v instanceof AllView) {
            // If this is the top-level (~not in a folder) AllView, show everything
            return List.of(new PeopleAction(j), new AsynchPeopleAction(j));
        }
        return List.of(new PeopleAction(v), new AsynchPeopleAction(v));
    }
}
