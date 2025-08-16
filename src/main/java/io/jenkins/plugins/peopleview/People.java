/*
 * The MIT License
 *
 * Copyright (c) 2004-2024, Sun Microsystems, Inc., Kohsuke Kawaguchi, Tom Huybrechts,
 * Yahoo!, Inc., Daniel Beck
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

import hudson.model.Api;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.ModelObject;
import hudson.model.Run;
import hudson.model.User;
import hudson.model.View;
import hudson.scm.ChangeLogSet;
import hudson.util.RunList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jenkins.model.Jenkins;
import jenkins.scm.RunWithSCM;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

@ExportedBean
public class People {
    @Exported
    public final List<UserInfo> users;

    public final ModelObject parent;

    public People(Jenkins parent, boolean loadPeople) {
        this.parent = parent;
        // for Hudson, really load all users
        if (loadPeople) {
            Map<User, UserInfo> users = getUserInfo(parent.getItems());
            User unknown = User.getUnknown();
            for (User u : User.getAll()) {
                if (u == unknown) continue; // skip the special 'unknown' user
                if (!users.containsKey(u)) users.put(u, new UserInfo(u, null, null));
            }
            this.users = toList(users);
        } else {
            this.users = Collections.emptyList();
        }
    }

    public People(View parent, boolean loadPeople) {
        this.parent = parent;
        if (loadPeople) {
            this.users = toList(getUserInfo(parent.getItems()));
        } else {
            this.users = Collections.emptyList();
        }
    }

    private Map<User, UserInfo> getUserInfo(Collection<? extends Item> items) {
        Map<User, UserInfo> users = new HashMap<>();
        for (Item item : items) {
            for (Job<?, ?> job : item.getAllJobs()) {
                RunList<? extends Run<?, ?>> runs = job.getBuilds();
                for (Run<?, ?> r : runs) {
                    if (r instanceof RunWithSCM) {
                        RunWithSCM<?, ?> runWithSCM = (RunWithSCM<?, ?>) r;

                        for (ChangeLogSet<? extends ChangeLogSet.Entry> c : runWithSCM.getChangeSets()) {
                            for (ChangeLogSet.Entry entry : c) {
                                User user = entry.getAuthor();

                                UserInfo info = users.get(user);
                                if (info == null) users.put(user, new UserInfo(user, job, r.getTimestamp()));
                                else if (info.getLastChange().before(r.getTimestamp())) {
                                    info.project = job;
                                    info.lastChange = r.getTimestamp();
                                }
                            }
                        }
                    }
                }
            }
        }
        return users;
    }

    private List<UserInfo> toList(Map<User, UserInfo> users) {
        ArrayList<UserInfo> list = new ArrayList<>(users.values());
        Collections.sort(list);
        return Collections.unmodifiableList(list);
    }

    public Api getApi() {
        if (parent instanceof Jenkins) {
            return new Api(new AsynchPeople((Jenkins) parent).new People());
        }
        return new Api(new AsynchPeople((View) parent).new People());
    }

    /**
     * @deprecated Potentially very expensive call; do not use from Jelly views.
     */
    @Deprecated
    public static boolean isApplicable(Collection<? extends Item> items) {
        for (Item item : items) {
            for (Job job : item.getAllJobs()) {
                RunList<? extends Run<?, ?>> runs = job.getBuilds();

                for (Run<?, ?> r : runs) {
                    if (r instanceof RunWithSCM) {
                        RunWithSCM<?, ?> runWithSCM = (RunWithSCM<?, ?>) r;
                        for (ChangeLogSet<? extends ChangeLogSet.Entry> c : runWithSCM.getChangeSets()) {
                            for (ChangeLogSet.Entry entry : c) {
                                User user = entry.getAuthor();
                                if (user != null) return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
}
