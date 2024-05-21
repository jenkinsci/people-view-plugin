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

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Functions;
import hudson.model.Api;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.ModelObject;
import hudson.model.Run;
import hudson.model.TopLevelItem;
import hudson.model.User;
import hudson.model.View;
import hudson.scm.ChangeLogSet;
import hudson.tasks.UserAvatarResolver;
import hudson.util.RunList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jenkins.model.Jenkins;
import jenkins.scm.RunWithSCM;
import jenkins.util.ProgressiveRendering;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.accmod.restrictions.suppressions.SuppressRestrictedWarnings;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

public class AsynchPeople extends ProgressiveRendering {

    private final Collection<TopLevelItem> items;
    private final User unknown;
    private final Map<User, UserInfo> users = new HashMap<>();
    private final Set<User> modified = new HashSet<>();
    private final String iconSize;
    public final ModelObject parent;

    public AsynchPeople(Jenkins parent) {
        this.parent = parent;
        items = parent.getItems();
        unknown = User.getUnknown();
    }

    public AsynchPeople(View parent) {
        this.parent = parent;
        items = parent.getItems();
        unknown = null;
    }

    @SuppressRestrictedWarnings(Functions.class)
    private static String getIconSize() {
        StaplerRequest req = Stapler.getCurrentRequest();
        return req != null ? Functions.validateIconSize(Functions.getCookie(req, "iconSize", "32x32")) : "32x32";
    }

    {
        iconSize = getIconSize();
    }

    @Override
    protected void compute() throws Exception {
        int itemCount = 0;
        for (Item item : items) {
            for (Job<?, ?> job : item.getAllJobs()) {
                RunList<? extends Run<?, ?>> builds = job.getBuilds();
                int buildCount = 0;
                for (Run<?, ?> r : builds) {
                    if (canceled()) {
                        return;
                    }
                    if (!(r instanceof RunWithSCM)) {
                        continue;
                    }

                    RunWithSCM<?, ?> runWithSCM = (RunWithSCM<?, ?>) r;
                    for (ChangeLogSet<? extends ChangeLogSet.Entry> c : runWithSCM.getChangeSets()) {
                        for (ChangeLogSet.Entry entry : c) {
                            User user = entry.getAuthor();
                            UserInfo info = users.get(user);
                            if (info == null) {
                                UserInfo userInfo = new UserInfo(user, job, r.getTimestamp());
                                userInfo.avatar = UserAvatarResolver.resolveOrNull(user, iconSize);
                                synchronized (this) {
                                    users.put(user, userInfo);
                                    modified.add(user);
                                }
                            } else if (info.getLastChange().before(r.getTimestamp())) {
                                synchronized (this) {
                                    info.project = job;
                                    info.lastChange = r.getTimestamp();
                                    modified.add(user);
                                }
                            }
                        }
                    }
                    // TODO consider also adding the user of the UserCause when applicable
                    buildCount++;
                    // TODO this defeats lazy-loading. Should rather do a breadth-first search, as in
                    // hudson.plugins.view.dashboard.builds.LatestBuilds
                    // (though currently there is no quick implementation of RunMap.size() ~ idOnDisk.size(), which
                    // would be needed for proper progress)
                    progress((itemCount + 1.0 * buildCount / builds.size()) / (items.size() + 1));
                }
            }
            itemCount++;
            progress(1.0 * itemCount / (items.size() + /* handling User.getAll */ 1));
        }
        if (unknown != null) {
            if (canceled()) {
                return;
            }
            for (User u : User.getAll()) { // TODO nice to have a method to iterate these lazily
                if (canceled()) {
                    return;
                }
                if (u == unknown) {
                    continue;
                }
                if (!users.containsKey(u)) {
                    UserInfo userInfo = new UserInfo(u, null, null);
                    userInfo.avatar = UserAvatarResolver.resolveOrNull(u, iconSize);
                    synchronized (this) {
                        users.put(u, userInfo);
                        modified.add(u);
                    }
                }
            }
        }
    }

    @NonNull
    @Override
    protected synchronized JSON data() {
        JSONArray r = new JSONArray();
        for (User u : modified) {
            UserInfo i = users.get(u);
            JSONObject entry = new JSONObject()
                    .accumulate("id", u.getId())
                    .accumulate("fullName", u.getFullName())
                    .accumulate("url", u.getUrl() + "/")
                    .accumulate(
                            "avatar",
                            i.avatar != null
                                    ? i.avatar
                                    : Stapler.getCurrentRequest().getContextPath() + Functions.getResourcePath()
                                            + "/images/svgs/person.svg")
                    .accumulate("timeSortKey", i.getTimeSortKey())
                    .accumulate("lastChangeTimeString", i.getLastChangeTimeString());
            Job<?, ?> p = i.getJob();
            if (p != null) {
                entry.accumulate("projectUrl", p.getUrl()).accumulate("projectFullDisplayName", p.getFullDisplayName());
            }
            r.add(entry);
        }
        modified.clear();
        return r;
    }

    public Api getApi() {
        return new Api(new AsynchPeople.InnerPeople());
    }

    /** JENKINS-16397 workaround */
    @Restricted(NoExternalUse.class)
    @ExportedBean
    public final class InnerPeople {

        private People people;

        @Exported
        public synchronized List<UserInfo> getUsers() {
            if (people == null) {
                people = parent instanceof Jenkins ? new People((Jenkins) parent) : new People((View) parent);
            }
            return people.users;
        }
    }
}
