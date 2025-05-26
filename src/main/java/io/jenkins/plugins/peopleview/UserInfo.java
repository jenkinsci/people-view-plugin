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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.model.User;
import java.util.Calendar;
import java.util.GregorianCalendar;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

@SuppressFBWarnings("EQ_COMPARETO_USE_OBJECT_EQUALS") // TODO Unchanged from core
@ExportedBean(defaultVisibility = 2)
public class UserInfo implements Comparable<UserInfo> {
    private final User user;
    /**
     * When did this user made a last commit on any of our projects? Can be null.
     */
    protected Calendar lastChange;
    /**
     * Which project did this user commit? Can be null.
     */
    protected Job<?, ?> project;

    /** @see hudson.tasks.UserAvatarResolver */
    String avatar;

    UserInfo(User user, Job<?, ?> p, Calendar lastChange) {
        this.user = user;
        this.project = p;
        this.lastChange = lastChange;
    }

    @Exported
    public User getUser() {
        return user;
    }

    @Exported
    public Calendar getLastChange() {
        return lastChange;
    }

    @Deprecated
    public AbstractProject getProject() {
        return project instanceof AbstractProject ? (AbstractProject) project : null;
    }

    @Exported(name = "project")
    public Job<?, ?> getJob() {
        return project;
    }

    /**
     * Returns a human-readable string representation of when this user was last active.
     */
    public String getLastChangeTimeString() {
        if (lastChange == null) return "N/A";
        long duration = new GregorianCalendar().getTimeInMillis() - ordinal();
        return Util.getTimeSpanString(duration);
    }

    public String getTimeSortKey() {
        if (lastChange == null) return "-";
        return Util.XS_DATETIME_FORMATTER2.format(lastChange.getTime().toInstant());
    }

    @Override
    public int compareTo(UserInfo that) {
        long rhs = that.ordinal();
        long lhs = this.ordinal();
        return Long.compare(rhs, lhs);
    }

    private long ordinal() {
        if (lastChange == null) return 0;
        return lastChange.getTimeInMillis();
    }
}
