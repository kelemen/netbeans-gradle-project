/**
 * Contains classes and interfaces related to displaying settings panels for
 * Gradle projects.
 * <P>
 * For project properties, you most likely want to use the
 * {@link org.netbeans.gradle.project.api.config.ui.ProfileBasedConfigurations#createProfileBasedCustomizer(org.netbeans.api.project.Project, org.netbeans.gradle.project.api.config.ui.CustomizerCategoryId, org.netbeans.gradle.project.api.config.ProjectSettingsProvider.ExtensionSettings, org.netbeans.gradle.project.api.config.ui.ProfileBasedSettingsPageFactory) ProfileBasedConfigurations.createProfileBasedCustomizer}
 * method.
 * <P>
 * Unlike most project types in NetBeans, the Gradle plugin supports using
 * different configuration for each profile.
 *
 * @see org.netbeans.gradle.project.api.config.ui.ProfileBasedConfigurations
 */
package org.netbeans.gradle.project.api.config.ui;
