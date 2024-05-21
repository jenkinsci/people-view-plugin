package io.jenkins.plugins.peopleview;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Action;
import hudson.model.View;
import java.util.Objects;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.StaplerProxy;

public class PeopleAction implements Action, StaplerProxy {

    private final View view;
    private final Jenkins jenkins;

    public PeopleAction(@NonNull View v) {
        this.view = v;
        this.jenkins = null;
    }

    public PeopleAction(@NonNull Jenkins j) {
        this.view = null;
        this.jenkins = j;
    }

    @Override
    public String getUrlName() {
        return "people";
    }

    @Override
    public String getDisplayName() {
        return Messages.People_DisplayName();
    }

    @Override
    public String getIconFileName() {
        return null; // no sidepanel link
    }

    @Override
    public Object getTarget() {
        if (view == null) {
            return new People(Objects.requireNonNull(jenkins));
        }
        return new People(view);
    }
}
