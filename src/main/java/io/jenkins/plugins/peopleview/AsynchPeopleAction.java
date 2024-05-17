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
