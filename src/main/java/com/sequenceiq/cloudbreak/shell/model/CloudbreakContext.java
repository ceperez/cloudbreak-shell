package com.sequenceiq.cloudbreak.shell.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.client.CloudbreakClient;

/**
 * Holds information about the connected Cloudbreak server.
 */
@Component
public class CloudbreakContext {

    private static final String ACCESSIBLE = "accessible";
    private Focus focus;
    private Hints hint;
    private Map<PropertyKey, String> properties = new HashMap<>();
    private Map<String, Map<Long, Integer>> instanceGroups = new HashMap<>();
    private List<Map<String, Object>> hostGroups = new ArrayList<>();
    private Set<String> activeHostgoups = new HashSet<>();
    private Set<String> activeTemplates = new HashSet<>();
    private Set<String> activeTemplateNames = new HashSet<>();
    private String activeCloudPlatform;

    @Autowired
    private CloudbreakClient client;

    public CloudbreakContext() {
        this.focus = getRootFocus();
        this.hint = Hints.NONE;
        this.instanceGroups = new HashMap<>();
        this.activeHostgoups = new HashSet<>();
    }

    public boolean isStackAvailable() {
        return isPropertyAvailable(PropertyKey.STACK_ID);
    }

    public void addStack(String id, String name) {
        addProperty(PropertyKey.STACK_ID, id);
        addProperty(PropertyKey.STACK_NAME, name);
        setStackAccessible();
    }

    public String getActiveCloudPlatform() {
        return this.activeCloudPlatform == null ? "" : this.activeCloudPlatform;
    }

    public void removeStack(String id) {
        removeProperty(PropertyKey.STACK_ID, id);
    }

    public void setInstanceGroups(Map<String, Map<Long, Integer>> instanceGroups) {
        this.instanceGroups = instanceGroups;
    }

    public void setHostGroups(List<Map<String, Object>> hostGroups) {
        this.hostGroups = hostGroups;
    }

    public Map<String, Map<Long, Integer>> getInstanceGroups() {
        return this.instanceGroups;
    }

    public List<Map<String, Object>> getHostGroups() {
        return hostGroups;
    }

    public Map<String, Object> getHostGroupByName(String name) {
        for (Map<String, Object> hostGroup : hostGroups) {
            if (hostGroup.get("name").equals(name)) {
                return hostGroup;
            }
        }
        return null;
    }

    public Map<String, Map<Long, Integer>> putInstanceGroup(String name, Map<Long, Integer> value) {
        this.instanceGroups.put(name, value);
        return this.instanceGroups;
    }

    public List<Map<String, Object>> putHostGroup(Map<String, Object> hostGroup) {
        this.hostGroups.add(hostGroup);
        return this.hostGroups;
    }

    public Set<String> getActiveTemplates() {
        return activeTemplates;
    }

    public Set<String> getActiveTemplateNames() {
        return activeTemplateNames;
    }

    public boolean isBlueprintAvailable() {
        return isPropertyAvailable(PropertyKey.BLUEPRINT_ID);
    }

    public void addBlueprint(String id) throws Exception {
        Map<String, Object> blueprintMap = client.getBlueprintMap(id);
        this.instanceGroups = new HashMap<>();
        this.hostGroups = new ArrayList<>();
        this.activeHostgoups = ((LinkedHashMap) blueprintMap.get("ambariBlueprint")).keySet();
        addProperty(PropertyKey.BLUEPRINT_ID, id);
        setBlueprintAccessible();
    }

    public boolean isCredentialAvailable() {
        return isPropertyAvailable(PropertyKey.CREDENTIAL_ID);
    }

    public void setCredential(String id) throws Exception {
        Map<String, String> credential = (Map<String, String>) client.getCredential(id);
        this.activeCloudPlatform = credential.get("cloudPlatform");
        Map<String, Map<String, String>> templateList = client.getAccountTemplatesWithCloudPlatformMap(this.activeCloudPlatform);
        fillTemplates(templateList);
        addProperty(PropertyKey.CREDENTIAL_ID, id);
        setCredentialAccessible();
    }

    private void fillTemplates(Map<String, Map<String, String>> templateList) {
        this.activeTemplates = templateList.keySet();
        for (Map.Entry templateEntry : templateList.entrySet()) {
            Map<String, String> templateMap = (Map<String, String>) templateEntry.getValue();
            this.activeTemplateNames.addAll(templateMap.keySet());
        }
    }

    public Set<String> getActiveHostgoups() {
        return activeHostgoups;
    }

    public void setBlueprintAccessible() {
        addProperty(PropertyKey.BLUEPRINT_ACCESSIBLE, ACCESSIBLE);
    }

    public boolean isBlueprintAccessible() {
        return isPropertyAvailable(PropertyKey.BLUEPRINT_ACCESSIBLE);
    }

    public void setCredentialAccessible() {
        addProperty(PropertyKey.CREDENTIAL_ACCESSIBLE, ACCESSIBLE);
    }

    public boolean isCredentialAccessible() {
        return isPropertyAvailable(PropertyKey.CREDENTIAL_ACCESSIBLE);
    }

    public void setStackAccessible() {
        addProperty(PropertyKey.STACK_ACCESSIBLE, ACCESSIBLE);
    }

    public boolean isStackAccessible() {
        return isPropertyAvailable(PropertyKey.STACK_ACCESSIBLE);
    }

    public void setRecipeAccessible() {
        addProperty(PropertyKey.RECIPE_ACCESSIBLE, ACCESSIBLE);
    }

    public boolean isRecipeAccessible() {
        return isPropertyAvailable(PropertyKey.RECIPE_ACCESSIBLE);
    }

    public String getStackId() {
        return getLastPropertyValue(PropertyKey.STACK_ID);
    }

    public String getStackName() {
        return getLastPropertyValue(PropertyKey.STACK_NAME);
    }

    public String getBlueprintId() {
        return getLastPropertyValue(PropertyKey.BLUEPRINT_ID);
    }

    public String getRecipeId() {
        return getLastPropertyValue(PropertyKey.RECIPE_ID);
    }

    public String getCredentialId() {
        return getLastPropertyValue(PropertyKey.CREDENTIAL_ID);
    }

    /**
     * Sets the focus to the root.
     */
    public void resetFocus() {
        this.focus = getRootFocus();
    }

    /**
     * Sets the focus.
     *
     * @param id   target of the focus
     * @param type type of the focus
     */
    public void setFocus(String id, FocusType type) {
        this.focus = new Focus(id, type);
    }

    /**
     * Returns the target of the focus.
     *
     * @return target
     */
    public String getFocusValue() {
        return focus.getValue();
    }

    /**
     * Sets what should be the next hint message.
     *
     * @param hint the new message
     */
    public void setHint(Hints hint) {
        this.hint = hint;
    }

    /**
     * Returns the context sensitive prompt.
     *
     * @return text of the prompt
     */
    public String getPrompt() {
        return focus.isType(FocusType.ROOT) ? "cloudbreak-shell>" : formatPrompt(focus.getPrefix(), focus.getValue());
    }

    /**
     * Returns some context sensitive hint.
     *
     * @return hint
     */
    public String getHint() {
        return "Hint: " + hint.message();
    }

    private boolean isFocusOn(FocusType type) {
        return focus.isType(type);
    }

    private Focus getRootFocus() {
        return new Focus("root", FocusType.ROOT);
    }

    private String formatPrompt(String prefix, String postfix) {
        return String.format("%s:%s>", prefix, postfix);
    }

    private boolean isPropertyAvailable(PropertyKey key) {
        return properties.get(key) != null && !properties.get(key).isEmpty();
    }

    private void addProperty(PropertyKey key, String value) {
        properties.remove(key);
        properties.put(key, value);
    }

    private void removeProperty(PropertyKey key, String value) {
        properties.remove(key);
    }

    private String getLastPropertyValue(PropertyKey key) {
        try {
            return properties.get(key);
        } catch (Exception ex) {
            return "";
        }
    }

    private enum PropertyKey {
        CREDENTIAL_ID, BLUEPRINT_ID, RECIPE_ID, TEMPLATE_ID, STACK_ID, STACK_NAME,
        CREDENTIAL_ACCESSIBLE, BLUEPRINT_ACCESSIBLE, TEMPLATE_ACCESSIBLE, STACK_ACCESSIBLE, RECIPE_ACCESSIBLE
    }
}
