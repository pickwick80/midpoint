/*
 * Copyright (c) 2010-2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.gui.api.page;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import com.evolveum.midpoint.web.application.DescriptorLoader;
import com.evolveum.midpoint.web.component.breadcrumbs.BreadcrumbPageInstance;
import com.evolveum.midpoint.web.page.login.PageLogin;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AdminGuiConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RichHyperlinkType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.devutils.debugbar.DebugBar;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.FeedbackMessages;
import org.apache.wicket.injection.Injector;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.resource.CoreLibrariesContributor;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.evolveum.midpoint.common.SystemConfigurationHolder;
import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.common.validator.EventHandler;
import com.evolveum.midpoint.common.validator.EventResult;
import com.evolveum.midpoint.common.validator.Validator;
import com.evolveum.midpoint.gui.api.component.result.OpResult;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.AccessCertificationService;
import com.evolveum.midpoint.model.api.ModelDiagnosticService;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.ScriptingService;
import com.evolveum.midpoint.model.api.TaskService;
import com.evolveum.midpoint.model.api.WorkflowService;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.report.api.ReportManager;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
import com.evolveum.midpoint.web.component.breadcrumbs.BreadcrumbPageClass;
import com.evolveum.midpoint.web.component.dialog.MainPopupDialog;
import com.evolveum.midpoint.web.component.menu.MainMenuItem;
import com.evolveum.midpoint.web.component.menu.MenuItem;
import com.evolveum.midpoint.web.component.menu.SideBarMenuItem;
import com.evolveum.midpoint.web.component.menu.SideBarMenuPanel;
import com.evolveum.midpoint.web.component.menu.UserMenuPanel;
import com.evolveum.midpoint.web.component.menu.top.LocalePanel;
import com.evolveum.midpoint.web.component.message.FeedbackAlerts;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.BreadcrumbItem;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.admin.PageAdminFocus;
import com.evolveum.midpoint.web.page.admin.certification.PageCertCampaigns;
import com.evolveum.midpoint.web.page.admin.certification.PageCertDecisions;
import com.evolveum.midpoint.web.page.admin.certification.PageCertDefinition;
import com.evolveum.midpoint.web.page.admin.certification.PageCertDefinitions;
import com.evolveum.midpoint.web.page.admin.configuration.PageAbout;
import com.evolveum.midpoint.web.page.admin.configuration.PageAccounts;
import com.evolveum.midpoint.web.page.admin.configuration.PageBulkAction;
import com.evolveum.midpoint.web.page.admin.configuration.PageDebugList;
import com.evolveum.midpoint.web.page.admin.configuration.PageDebugView;
import com.evolveum.midpoint.web.page.admin.configuration.PageImportObject;
import com.evolveum.midpoint.web.page.admin.configuration.PageInternals;
import com.evolveum.midpoint.web.page.admin.configuration.PageRepoQuery;
import com.evolveum.midpoint.web.page.admin.configuration.PageSystemConfiguration;
import com.evolveum.midpoint.web.page.admin.home.PageDashboard;
import com.evolveum.midpoint.web.page.admin.reports.PageCreatedReports;
import com.evolveum.midpoint.web.page.admin.reports.PageNewReport;
import com.evolveum.midpoint.web.page.admin.reports.PageReport;
import com.evolveum.midpoint.web.page.admin.reports.PageReports;
import com.evolveum.midpoint.web.page.admin.resources.PageImportResource;
import com.evolveum.midpoint.web.page.admin.resources.PageResource;
import com.evolveum.midpoint.web.page.admin.resources.PageResourceWizard;
import com.evolveum.midpoint.web.page.admin.resources.PageResources;
import com.evolveum.midpoint.web.page.admin.roles.PageRole;
import com.evolveum.midpoint.web.page.admin.roles.PageRoles;
import com.evolveum.midpoint.web.page.admin.server.PageTaskAdd;
import com.evolveum.midpoint.web.page.admin.server.PageTaskEdit;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.page.admin.users.PageOrgTree;
import com.evolveum.midpoint.web.page.admin.users.PageOrgUnit;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.web.page.admin.users.PageUsers;
import com.evolveum.midpoint.web.page.admin.workflow.PageProcessInstancesAll;
import com.evolveum.midpoint.web.page.admin.workflow.PageProcessInstancesRequestedBy;
import com.evolveum.midpoint.web.page.admin.workflow.PageProcessInstancesRequestedFor;
import com.evolveum.midpoint.web.page.admin.workflow.PageWorkItems;
import com.evolveum.midpoint.web.page.admin.workflow.PageWorkItemsClaimable;
import com.evolveum.midpoint.web.page.self.PageSelfCredentials;
import com.evolveum.midpoint.web.page.self.PageSelfDashboard;
import com.evolveum.midpoint.web.page.self.PageSelfProfile;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.security.MidPointAuthWebSession;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.web.security.WebApplicationConfiguration;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.validation.MidpointFormValidatorRegistry;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author lazyman
 * @author semancik
 */
public abstract class PageBase extends WebPage implements ModelServiceLocator {

	private static final String DOT_CLASS = PageBase.class.getName() + ".";
	private static final String OPERATION_LOAD_USER = DOT_CLASS + "loadUser";

	private static final String ID_TITLE = "title";
	private static final String ID_PAGE_TITLE_CONTAINER = "pageTitleContainer";
	private static final String ID_PAGE_TITLE_REAL = "pageTitleReal";
	private static final String ID_PAGE_TITLE = "pageTitle";
	private static final String ID_DEBUG_PANEL = "debugPanel";
	private static final String ID_VERSION = "version";
	public static final String ID_FEEDBACK_CONTAINER = "feedbackContainer";
	private static final String ID_FEEDBACK = "feedback";
	private static final String ID_TEMP_FEEDBACK = "tempFeedback";
	private static final String ID_DEBUG_BAR = "debugBar";
	private static final String ID_CLEAR_CACHE = "clearCssCache";
	private static final String ID_FEEDBACK_LIST = "feedbackList";
	private static final String ID_FEEDBACK_DETAILS = "feedbackDetails";
	private static final String ID_SIDEBAR_MENU = "sidebarMenu";
	private static final String ID_RIGHT_MENU = "rightMenu";
	private static final String ID_LOCALE = "locale";
	private static final String ID_MENU_TOGGLE = "menuToggle";
	private static final String ID_BREADCRUMBS = "breadcrumbs";
	private static final String ID_BREADCRUMB = "breadcrumb";
	private static final String ID_BC_LINK = "bcLink";
	private static final String ID_BC_ICON = "bcIcon";
	private static final String ID_BC_NAME = "bcName";
	private static final String ID_MAIN_POPUP = "mainPopup";
	private static final String ID_MAIN_POPUP_BODY = "popupBody";
    private static final String OPERATION_GET_SYSTEM_CONFIG = DOT_CLASS + "getSystemConfiguration";

	private static final Trace LOGGER = TraceManager.getTrace(PageBase.class);

	@SpringBean(name = "modelController")
	private ScriptingService scriptingService;

	@SpringBean(name = "modelController")
	private ModelService modelService;

	@SpringBean(name = "modelInteractionService")
	private ModelInteractionService modelInteractionService;

	@SpringBean(name = "modelController")
	private TaskService taskService;

	@SpringBean(name = "modelDiagController")
	private ModelDiagnosticService modelDiagnosticService;

	@SpringBean(name = "taskManager")
	private TaskManager taskManager;

	@SpringBean(name = "modelController")
	private WorkflowService workflowService;

	@SpringBean(name = "workflowManager")
	private WorkflowManager workflowManager;

	@SpringBean(name = "midpointConfiguration")
	private MidpointConfiguration midpointConfiguration;

	@SpringBean(name = "reportManager")
	private ReportManager reportManager;

	// @SpringBean(name = "certificationManager")
	// private CertificationManager certificationManager;

	@SpringBean(name = "modelController")
	private AccessCertificationService certficationService;

	@SpringBean(name = "accessDecisionManager")
	private SecurityEnforcer securityEnforcer;

	@SpringBean
	private MidpointFormValidatorRegistry formValidatorRegistry;

	private PageBase previousPage; // experimental -- where to return e.g. when
									// 'Back' button is clicked [NOT a class, in
									// order to eliminate reinitialization when
									// it is not needed]
	private boolean reinitializePreviousPages; // experimental -- should we
												// reinitialize all the chain of
												// previous pages?

	private boolean initialized = false;

	public PageBase(PageParameters parameters) {
		super(parameters);

		Injector.get().inject(this);
		Validate.notNull(modelService, "Model service was not injected.");
		Validate.notNull(taskManager, "Task manager was not injected.");
		Validate.notNull(reportManager, "Report manager was not injected.");

		initLayout();
	}

	@Override
	protected void onConfigure() {
		super.onConfigure();

		if (initialized) {
			return;
		}
		initialized = true;

		createBreadcrumb();
	}

	protected void createBreadcrumb() {
		BreadcrumbPageClass bc = new BreadcrumbPageClass(new AbstractReadOnlyModel() {

			@Override
			public String getObject() {
				return getPageTitleModel().getObject();
			}
		}, this.getClass(), getPageParameters());

		getSessionStorage().pushBreadcrumb(bc);
	}

	protected void createInstanceBreadcrumb() {
		BreadcrumbPageInstance bc = new BreadcrumbPageInstance(new AbstractReadOnlyModel() {

			@Override
			public String getObject() {
				return getPageTitleModel().getObject();
			}
		}, this);

		getSessionStorage().pushBreadcrumb(bc);
	}

	public PageBase() {
		this(null);
	}

	public MidPointApplication getMidpointApplication() {
		return (MidPointApplication) getApplication();
	}

	public WebApplicationConfiguration getWebApplicationConfiguration() {
		MidPointApplication application = getMidpointApplication();
		return application.getWebApplicationConfiguration();
	}

	public PrismContext getPrismContext() {
		return getMidpointApplication().getPrismContext();
	}

	public TaskManager getTaskManager() {
		return taskManager;
	}

	protected WorkflowService getWorkflowService() {
		return workflowService;
	}

	public WorkflowManager getWorkflowManager() {
		return workflowManager;
	}

	public ReportManager getReportManager() {
		return reportManager;
	}

	public AccessCertificationService getCertificationService() {
		return certficationService;
	}

	@Override
	public ModelService getModelService() {
		return modelService;
	}

	public ScriptingService getScriptingService() {
		return scriptingService;
	}

	public TaskService getTaskService() {
		return taskService;
	}

	public SecurityEnforcer getSecurityEnforcer() {
		return securityEnforcer;
	}

	@Override
	public ModelInteractionService getModelInteractionService() {
		return modelInteractionService;
	}

	protected ModelDiagnosticService getModelDiagnosticService() {
		return modelDiagnosticService;
	}

	public MidpointFormValidatorRegistry getFormValidatorRegistry() {
		return formValidatorRegistry;
	}

	// public static StringResourceModel createStringResourceStatic(Component
	// component, String resourceKey, Object... objects) {
	// return new StringResourceModel(resourceKey, component, new
	// Model<String>(), resourceKey, objects);
	// }

	public static StringResourceModel createStringResourceStatic(Component component, Enum e) {
		String resourceKey = createEnumResourceKey(e);
		return createStringResourceStatic(component, resourceKey);
	}

	public static String createEnumResourceKey(Enum e) {
		return e.getDeclaringClass().getSimpleName() + "." + e.name();
	}

	public Task createSimpleTask(String operation, PrismObject<UserType> owner) {
		TaskManager manager = getTaskManager();
		Task task = manager.createTaskInstance(operation);

		if (owner == null) {
			MidPointPrincipal user = SecurityUtils.getPrincipalUser();
			if (user == null) {
				throw new RestartResponseException(PageLogin.class);
			} else {
				owner = user.getUser().asPrismObject();
			}
		}

		task.setOwner(owner);
		task.setChannel(SchemaConstants.CHANNEL_GUI_USER_URI);

		return task;
	}

	public Task createSimpleTask(String operation) {
		MidPointPrincipal user = SecurityUtils.getPrincipalUser();
		if (user == null) {
			throw new RestartResponseException(PageLogin.class);
		}
		return createSimpleTask(operation, user.getUser().asPrismObject());
	}

	public MidpointConfiguration getMidpointConfiguration() {
		return midpointConfiguration;
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);

		// this attaches jquery.js as first header item, which is used in our
		// scripts.
		CoreLibrariesContributor.contribute(getApplication(), response);
	}

	@Override
	protected void onBeforeRender() {
		super.onBeforeRender();
		FeedbackMessages messages = getSession().getFeedbackMessages();
		for (FeedbackMessage message : messages) {
			getFeedbackMessages().add(message);
		}

		getSession().getFeedbackMessages().clear();
	}

	private void initHeaderLayout() {
		WebMarkupContainer menuToggle = new WebMarkupContainer(ID_MENU_TOGGLE);
		menuToggle.add(createUserStatusBehaviour(true));
		add(menuToggle);

		UserMenuPanel rightMenu = new UserMenuPanel(ID_RIGHT_MENU);
		rightMenu.add(createUserStatusBehaviour(true));
		add(rightMenu);

		LocalePanel locale = new LocalePanel(ID_LOCALE);
		locale.add(createUserStatusBehaviour(false));
		add(locale);
	}

	private void initTitleLayout() {
		WebMarkupContainer pageTitleContainer = new WebMarkupContainer(ID_PAGE_TITLE_CONTAINER);
		pageTitleContainer.add(createUserStatusBehaviour(true));
		add(pageTitleContainer);

		WebMarkupContainer pageTitle = new WebMarkupContainer(ID_PAGE_TITLE);
		pageTitleContainer.add(pageTitle);
		Label pageTitleReal = new Label(ID_PAGE_TITLE_REAL, createPageTitleModel());
		pageTitleReal.setRenderBodyOnly(true);
		pageTitle.add(pageTitleReal);

		ListView breadcrumbs = new ListView<Breadcrumb>(ID_BREADCRUMB,
				new AbstractReadOnlyModel<List<Breadcrumb>>() {

					@Override
					public List<Breadcrumb> getObject() {
						return getSessionStorage().getBreadcrumbs();
					}
				}) {

            @Override
            protected void populateItem(ListItem<Breadcrumb> item) {
                final Breadcrumb dto = item.getModelObject();

				AjaxLink bcLink = new AjaxLink(ID_BC_LINK) {

					@Override
					public void onClick(AjaxRequestTarget target) {
						redirectBackToBreadcrumb(dto);
					}
				};
				item.add(bcLink);
                bcLink.add(new VisibleEnableBehaviour() {

                    @Override
                    public boolean isEnabled() {
                        return dto.isUseLink();
                    }
                });

				WebMarkupContainer bcIcon = new WebMarkupContainer(ID_BC_ICON);
				bcIcon.add(new VisibleEnableBehaviour() {

					@Override
					public boolean isVisible() {
						return dto.getIcon() != null && dto.getIcon().getObject() != null;
					}
				});
				bcIcon.add(AttributeModifier.replace("class", dto.getIcon()));
				bcLink.add(bcIcon);

                Label bcName = new Label(ID_BC_NAME, dto.getLabel());
				bcLink.add(bcName);

				item.add(new VisibleEnableBehaviour() {

					@Override
					public boolean isVisible() {
						return dto.isVisible();
					}
				});
			}
		};
        add(breadcrumbs);
	}

	private void initLayout() {
		Label title = new Label(ID_TITLE, createPageTitleModel());
		title.setRenderBodyOnly(true);
		add(title);

		initHeaderLayout();
		initTitleLayout();
		initDebugBarLayout();

		List<SideBarMenuItem> menuItems = createMenuItems();
		SideBarMenuPanel sidebarMenu = new SideBarMenuPanel(ID_SIDEBAR_MENU, new Model((Serializable) menuItems));
		sidebarMenu.add(createUserStatusBehaviour(true));
		add(sidebarMenu);

		WebMarkupContainer version = new WebMarkupContainer(ID_VERSION) {
			@Deprecated
			public String getDescribe() {
				return PageBase.this.getDescribe();
			}
		};
		version.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return RuntimeConfigurationType.DEVELOPMENT.equals(getApplication().getConfigurationType());
			}
		});
		add(version);

		WebMarkupContainer feedbackContainer = new WebMarkupContainer(ID_FEEDBACK_CONTAINER);
		feedbackContainer.setOutputMarkupId(true);
		add(feedbackContainer);

		FeedbackAlerts feedbackList = new FeedbackAlerts(ID_FEEDBACK);
		feedbackList.setOutputMarkupId(true);
		feedbackContainer.add(feedbackList);

		MainPopupDialog mainPopup = new MainPopupDialog(ID_MAIN_POPUP);
		add(mainPopup);
	}

	public MainPopupDialog getMainPopup() {
		return (MainPopupDialog) get(ID_MAIN_POPUP);
	}

	public String getMainPopupBodyId() {
		return ID_MAIN_POPUP_BODY;
	}

	public void setMainPopupTitle(IModel<String> title) {
		getMainPopup().setTitle(title);
	}

	public void setMainPopupContent(Component body) {
		getMainPopup().setBody(body);
	}

	public void showMainPopup(AjaxRequestTarget target) {
		getMainPopup().show(target);
	}

    public void showMainPopup(Component body, IModel<String> title, AjaxRequestTarget target) {
        setMainPopupContent(body);
        setMainPopupTitle(title);
        showMainPopup(target);
    }
    
    public void showMainPopup(Component body, IModel<String> title, AjaxRequestTarget target, int initialWidth, int initialHeight) {
       getMainPopup().setInitialHeight(initialHeight);
       getMainPopup().setInitialWidth(initialWidth);
       showMainPopup(body, title, target);
    }

    public void hideMainPopup(AjaxRequestTarget target) {
        getMainPopup().close(target);
    }

    private VisibleEnableBehaviour createUserStatusBehaviour(final boolean visibleIfLoggedIn) {
		return new VisibleEnableBehaviour() {

			@Override
			public boolean isVisible() {
				return SecurityUtils.getPrincipalUser() != null ? visibleIfLoggedIn : !visibleIfLoggedIn;
			}
		};
	}

	private void initDebugBarLayout() {
		DebugBar debugPanel = new DebugBar(ID_DEBUG_PANEL);
		add(debugPanel);

		WebMarkupContainer debugBar = new WebMarkupContainer(ID_DEBUG_BAR);
		debugBar.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isVisible() {
				RuntimeConfigurationType runtime = getApplication().getConfigurationType();
				return RuntimeConfigurationType.DEVELOPMENT.equals(runtime);
			}
		});
		add(debugBar);

		AjaxButton clearCache = new AjaxButton(ID_CLEAR_CACHE, createStringResource("PageBase.clearCssCache")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				clearLessJsCache(target);
			}
		};
		debugBar.add(clearCache);
	}

	protected void clearLessJsCache(AjaxRequestTarget target) {
		try {
			ArrayList<MBeanServer> servers = MBeanServerFactory.findMBeanServer(null);
			if (servers.size() > 1) {
				LOGGER.info("Too many mbean servers, cache won't be cleared.");
				for (MBeanServer server : servers) {
					LOGGER.info(server.getDefaultDomain());
				}
				return;
			}
			MBeanServer server = servers.get(0);
			ObjectName objectName = ObjectName.getInstance("wro4j-idm:type=WroConfiguration");
			server.invoke(objectName, "reloadCache", new Object[] {}, new String[] {});
			if (target != null) {
				target.add(PageBase.this);
			}
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't clear less/js cache", ex);
			error("Error occurred, reason: " + ex.getMessage());
			if (target != null) {
				target.add(getFeedbackPanel());
			}
		}
	}

	public WebMarkupContainer getFeedbackPanel() {
		return (WebMarkupContainer) get(ID_FEEDBACK_CONTAINER);
	}

	public SessionStorage getSessionStorage() {
		MidPointAuthWebSession session = (MidPointAuthWebSession) getSession();
		return session.getSessionStorage();
	}

	protected IModel<String> createPageTitleModel() {
		String key = getClass().getSimpleName() + ".title";
		return createStringResource(key);
	}

    public IModel<String> getPageTitleModel() {
        return (IModel) get(ID_TITLE).getDefaultModel();
    }

	public String getString(String resourceKey, Object... objects) {
		return createStringResource(resourceKey, objects).getString();
	}

	public StringResourceModel createStringResource(String resourceKey, Object... objects) {
		return new StringResourceModel(resourceKey, this).setModel(new Model<String>()).setDefaultValue(resourceKey)
				.setParameters(objects);
	}

	public StringResourceModel createStringResource(Enum e) {
		String resourceKey = e.getDeclaringClass().getSimpleName() + "." + e.name();
		return createStringResource(resourceKey);
	}

    public static StringResourceModel createStringResourceStatic(Component component, String resourceKey,
                                                                 Object... objects) {
        return new StringResourceModel(resourceKey, component).setModel(new Model<String>())
                .setDefaultValue(resourceKey).setParameters(objects);
    }

    public void showResult(OperationResult result, String errorMessageKey) {
        showResult(result, errorMessageKey, true);
    }

    public void showResult(OperationResult result, boolean showSuccess) {
        showResult(result, null, showSuccess);
    }

    public void showResult(OperationResult result) {
        showResult(result, null, true);
    }

    public void showResult(OperationResult result, String errorMessageKey, boolean showSuccess) {
        Validate.notNull(result, "Operation result must not be null.");
        Validate.notNull(result.getStatus(), "Operation result status must not be null.");

        OpResult opResult = OpResult.getOpResult((PageBase) getPage(), result);
        switch (opResult.getStatus()) {
            case FATAL_ERROR:
            case PARTIAL_ERROR:
                getSession().error(opResult);

                break;
            case IN_PROGRESS:
            case NOT_APPLICABLE:
                getSession().info(opResult);
                break;
            case SUCCESS:
                if (!showSuccess) {
                    break;
                }
                getSession().success(opResult);

                break;
            case UNKNOWN:
            case WARNING:
            default:
                getSession().warn(opResult);

        }

    }

   
	public String createComponentPath(String... components) {
		return StringUtils.join(components, ":");
	}

	/**
	 * It's here only because of some IDEs - it's not properly filtering
	 * resources during maven build. "describe" variable is not replaced.
	 *
	 * @return "unknown" instead of "git describe" for current build.
	 */
	@Deprecated
	public String getDescribe() {
		return getString("pageBase.unknownBuildNumber");
	}

	protected ModalWindow createModalWindow(final String id, IModel<String> title, int width, int height) {
		final ModalWindow modal = new ModalWindow(id);
		add(modal);

		modal.setResizable(false);
		modal.setTitle(title);
		modal.setCookieName(PageBase.class.getSimpleName() + ((int) (Math.random() * 100)));

		modal.setInitialWidth(width);
		modal.setWidthUnit("px");
		modal.setInitialHeight(height);
		modal.setHeightUnit("px");

		modal.setCloseButtonCallback(new ModalWindow.CloseButtonCallback() {

			@Override
			public boolean onCloseButtonClicked(AjaxRequestTarget target) {
				return true;
			}
		});

		modal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

			@Override
			public void onClose(AjaxRequestTarget target) {
				modal.close(target);
			}
		});

		modal.add(new AbstractDefaultAjaxBehavior() {

			@Override
			public void renderHead(Component component, IHeaderResponse response) {
				response.render(OnDomReadyHeaderItem.forScript("Wicket.Window.unloadConfirmation = false;"));
				response.render(JavaScriptHeaderItem
						.forScript("$(document).ready(function() {\n" + "  $(document).bind('keyup', function(evt) {\n"
								+ "    if (evt.keyCode == 27) {\n" + getCallbackScript() + "\n"
								+ "        evt.preventDefault();\n" + "    }\n" + "  });\n" + "});", id));
			}

			@Override
			protected void respond(AjaxRequestTarget target) {
				modal.close(target);
			}
		});

		return modal;
	}

	public boolean isReinitializePreviousPages() {
		return reinitializePreviousPages;
	}

	public void setReinitializePreviousPages(boolean reinitializePreviousPages) {
		this.reinitializePreviousPages = reinitializePreviousPages;
	}

	public PageBase getPreviousPage() {
		return previousPage;
	}

	public void setPreviousPage(PageBase previousPage) {
		this.previousPage = previousPage;
	}

	// experimental -- all pages should know how to reinitialize themselves
	// (most hardcore way is to construct a new instance of themselves)
	public PageBase reinitialize() {
		// by default there is nothing to do -- our pages have to know how to
		// reinitialize themselves
		LOGGER.trace("Default no-op implementation of reinitialize() called.");
		return this;
	}

	// experimental -- go to previous page (either with reinitialization e.g.
	// when something changed, or without - typically when 'back' button is
	// pressed)
	protected void goBack(Class<? extends Page> defaultBackPageClass) {
		LOGGER.trace("goBack called; page = {}, previousPage = {}, reinitializePreviousPages = {}",
				new Object[] { this, previousPage, reinitializePreviousPages });
		if (previousPage != null) {
			setResponsePage(getPreviousPageToGoTo());
		} else {
			LOGGER.trace("...going to default back page {}", defaultBackPageClass);
			setResponsePage(defaultBackPageClass);
		}
	}

	// returns previous page ready to go to (i.e. reinitialized, if necessary)
	public PageBase getPreviousPageToGoTo() {
		if (previousPage == null) {
			return null;
		}

		if (isReinitializePreviousPages()) {
			LOGGER.trace("...calling reinitialize on previousPage ({})", previousPage);

			previousPage.setReinitializePreviousPages(true); // we set this flag
																// on the
																// original
																// previous
																// page...
			PageBase reinitialized = previousPage.reinitialize();
			reinitialized.setReinitializePreviousPages(true); // ...but on the
																// returned
																// value, as it
																// is probably
																// different
																// object
			return reinitialized;
		} else {
			return previousPage;
		}
	}

	// returns to previous page via restart response exception
	public RestartResponseException getRestartResponseException(Class<? extends Page> defaultBackPageClass) {
		LOGGER.trace("getRestartResponseException called; page = {}, previousPage = {}, reinitializePreviousPages = {}",
				new Object[] { this, previousPage, reinitializePreviousPages });
		if (previousPage != null) {
			return new RestartResponseException(getPreviousPageToGoTo());
		} else {
			LOGGER.trace("...going to default back page {}", defaultBackPageClass);
			return new RestartResponseException(defaultBackPageClass);
		}
	}

	protected <P extends Object> void validateObject(String xmlObject, final Holder<P> objectHolder,
			boolean validateSchema, OperationResult result) {
		EventHandler handler = new EventHandler() {

			@Override
			public EventResult preMarshall(Element objectElement, Node postValidationTree,
					OperationResult objectResult) {
				return EventResult.cont();
			}

			@Override
			public <T extends Objectable> EventResult postMarshall(PrismObject<T> object, Element objectElement,
					OperationResult objectResult) {
				objectHolder.setValue((P) object);
				return EventResult.cont();
			}

			@Override
			public void handleGlobalError(OperationResult currentResult) {
			}
		};
		Validator validator = new Validator(getPrismContext(), handler);
		validator.setVerbose(true);
		validator.setValidateSchema(validateSchema);
		validator.validateObject(xmlObject, result);

		result.computeStatus();
	}

	public long getItemsPerPage(UserProfileStorage.TableId tableId) {
		UserProfileStorage userProfile = getSessionStorage().getUserProfile();
		return userProfile.getPagingSize(tableId);
	}

	protected List<SideBarMenuItem> createMenuItems() {
		List<SideBarMenuItem> menus = new ArrayList<>();

		SideBarMenuItem menu = new SideBarMenuItem(createStringResource("PageAdmin.menu.selfService"));
		menus.add(menu);
		createSelfServiceMenu(menu);

		menu = new SideBarMenuItem(createStringResource("PageAdmin.menu.mainNavigation"));
		menus.add(menu);
		List<MainMenuItem> items = menu.getItems();

        menu = new SideBarMenuItem(createStringResource("PageAdmin.menu.additional"));
        menus.add(menu);
        createAdditionalMenu(menu);

		// todo fix with visible behaviour [lazyman]
		if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_DASHBOARD_URL,
				AuthorizationConstants.AUTZ_UI_HOME_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL,
				AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL)) {
			items.add(createHomeItems());
		}

		if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_USERS_URL,
				AuthorizationConstants.AUTZ_UI_USERS_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL,
				AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL)) {
			items.add(createUsersItems());
		}

		if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ORG_STRUCT_URL,
				AuthorizationConstants.AUTZ_UI_ORG_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL,
				AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL)) {
			items.add(createOrganizationsMenu());
		}

		if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ROLES_URL,
				AuthorizationConstants.AUTZ_UI_ROLES_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL,
				AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL)) {
			items.add(createRolesItems());
		}

		if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_RESOURCES_URL,
				AuthorizationConstants.AUTZ_UI_RESOURCES_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL,
				AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL, AuthorizationConstants.AUTZ_UI_RESOURCE_URL,
				AuthorizationConstants.AUTZ_UI_RESOURCE_EDIT_URL)) {
			items.add(createResourcesItems());
		}

		if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_WORK_ITEMS_URL,
				AuthorizationConstants.AUTZ_UI_WORK_ITEMS_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL,
				AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL)) {
			if (getWorkflowManager().isEnabled()) {
				items.add(createWorkItemsItems());
			}
		}

		if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_CERTIFICATION_ALL_URL,
				AuthorizationConstants.AUTZ_UI_CERTIFICATION_DEFINITIONS_URL,
				AuthorizationConstants.AUTZ_UI_CERTIFICATION_NEW_DEFINITION_URL,
				AuthorizationConstants.AUTZ_UI_CERTIFICATION_CAMPAIGNS_URL,
				AuthorizationConstants.AUTZ_UI_CERTIFICATION_DECISIONS_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL,
				AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL)) {
			items.add(createCertificationItems());
		}

		if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_TASKS_URL,
				AuthorizationConstants.AUTZ_UI_TASKS_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL,
				AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL)) {
			items.add(createServerTasksItems());
		}

		if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_REPORTS_URL,
				AuthorizationConstants.AUTZ_UI_REPORTS_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL)) {
			items.add(createReportsItems());
		}

		if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_CONFIGURATION_URL,
				AuthorizationConstants.AUTZ_UI_CONFIGURATION_DEBUG_URL,
				AuthorizationConstants.AUTZ_UI_CONFIGURATION_DEBUGS_URL,
				AuthorizationConstants.AUTZ_UI_CONFIGURATION_IMPORT_URL,
				AuthorizationConstants.AUTZ_UI_CONFIGURATION_LOGGING_URL,
				AuthorizationConstants.AUTZ_UI_CONFIGURATION_SYSTEM_CONFIG_URL,
				AuthorizationConstants.AUTZ_UI_CONFIGURATION_ABOUT_URL,
				AuthorizationConstants.AUTZ_UI_CONFIGURATION_SYNCHRONIZATION_ACCOUNTS_URL,
				AuthorizationConstants.AUTZ_UI_CONFIGURATION_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL,
				AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL)) {
			items.add(createConfigurationItems());
		}

		return menus;
	}

	private MainMenuItem createWorkItemsItems() {
		MainMenuItem item = new MainMenuItem("fa fa-inbox", createStringResource("PageAdmin.menu.top.workItems"), null);

		List<MenuItem> submenu = item.getItems();

		MenuItem menu = new MenuItem(createStringResource("PageAdmin.menu.top.workItems.list"), PageWorkItems.class);
		submenu.add(menu);
		menu = new MenuItem(createStringResource("PageAdmin.menu.top.workItems.listClaimable"),
				PageWorkItemsClaimable.class);
		submenu.add(menu);

		menu = new MenuItem(createStringResource("PageAdmin.menu.top.workItems.listProcessInstancesAll"),
				PageProcessInstancesAll.class);
		submenu.add(menu);
		menu = new MenuItem(createStringResource("PageAdmin.menu.top.workItems.listProcessInstancesRequestedBy"),
				PageProcessInstancesRequestedBy.class);
		submenu.add(menu);
		menu = new MenuItem(createStringResource("PageAdmin.menu.top.workItems.listProcessInstancesRequestedFor"),
				PageProcessInstancesRequestedFor.class);
		submenu.add(menu);

		return item;
	}

	private MainMenuItem createServerTasksItems() {
		MainMenuItem item = new MainMenuItem("fa fa-tasks", createStringResource("PageAdmin.menu.top.serverTasks"),
				null);

		List<MenuItem> submenu = item.getItems();

		MenuItem list = new MenuItem(createStringResource("PageAdmin.menu.top.serverTasks.list"), PageTasks.class, null,
				null);
		submenu.add(list);
		MenuItem n = new MenuItem(createStringResource("PageAdmin.menu.top.serverTasks.new"), PageTaskAdd.class);
		submenu.add(n);
		n = new MenuItem(createStringResource("PageAdmin.menu.top.serverTasks.edit"), PageTaskEdit.class, null,
				createVisibleDisabledBehaviorForEditMenu(PageTaskEdit.class));
		submenu.add(n);

		return item;
	}

	private MainMenuItem createResourcesItems() {
		MainMenuItem item = new MainMenuItem("fa fa-laptop", createStringResource("PageAdmin.menu.top.resources"),
				null);

		List<MenuItem> submenu = item.getItems();

		MenuItem list = new MenuItem(createStringResource("PageAdmin.menu.top.resources.list"), PageResources.class);
		submenu.add(list);
		createFocusPageViewMenu(submenu, "PageAdmin.menu.top.resources.view", PageResource.class);
		createFocusPageNewEditMenu(submenu, "PageAdmin.menu.top.resources.new", "PageAdmin.menu.top.resources.edit",
				PageResourceWizard.class);
		MenuItem n = new MenuItem(createStringResource("PageAdmin.menu.top.resources.import"),
				PageImportResource.class);
		submenu.add(n);

		return item;
	}

	private MainMenuItem createReportsItems() {
		MainMenuItem item = new MainMenuItem("fa fa-pie-chart", createStringResource("PageAdmin.menu.top.reports"),
				null);

		List<MenuItem> submenu = item.getItems();

		MenuItem list = new MenuItem(createStringResource("PageAdmin.menu.top.reports.list"), PageReports.class);
		submenu.add(list);
		MenuItem configure = new MenuItem(createStringResource("PageAdmin.menu.top.reports.configure"),
				PageReport.class, null, createVisibleDisabledBehaviorForEditMenu(PageReport.class));
		submenu.add(configure);
		MenuItem created = new MenuItem(createStringResource("PageAdmin.menu.top.reports.created"),
				PageCreatedReports.class);
		submenu.add(created);
		MenuItem n = new MenuItem(createStringResource("PageAdmin.menu.top.reports.new"), PageNewReport.class);
		submenu.add(n);

		return item;
	}

	private MainMenuItem createCertificationItems() {

		MainMenuItem item = new MainMenuItem("fa fa-certificate",
				createStringResource("PageAdmin.menu.top.certification"), null);

		List<MenuItem> submenu = item.getItems();

		MenuItem menu = new MenuItem(createStringResource("PageAdmin.menu.top.certification.definitions"),
				PageCertDefinitions.class);
		submenu.add(menu);
		menu = new MenuItem(createStringResource("PageAdmin.menu.top.certification.newDefinition"),
				PageCertDefinition.class);
		submenu.add(menu);
		menu = new MenuItem(createStringResource("PageAdmin.menu.top.certification.campaigns"),
				PageCertCampaigns.class);
		submenu.add(menu);
		PageParameters params = new PageParameters();
		params.add(PageTasks.SELECTED_CATEGORY, TaskCategory.ACCESS_CERTIFICATION);
		menu = new MenuItem(createStringResource("PageAdmin.menu.top.certification.scheduling"), PageTasks.class,
				params, null);
		submenu.add(menu);
		menu = new MenuItem(createStringResource("PageAdmin.menu.top.certification.decisions"),
				PageCertDecisions.class);
		submenu.add(menu);

		return item;
	}

	private MainMenuItem createConfigurationItems() {
		MainMenuItem item = new MainMenuItem("fa fa-cog", createStringResource("PageAdmin.menu.top.configuration"),
				null);

		List<MenuItem> submenu = item.getItems();

		MenuItem menu = new MenuItem(createStringResource("PageAdmin.menu.top.configuration.bulkActions"),
				PageBulkAction.class);
		submenu.add(menu);
		menu = new MenuItem(createStringResource("PageAdmin.menu.top.configuration.importObject"),
				PageImportObject.class);
		submenu.add(menu);
		menu = new MenuItem(createStringResource("PageAdmin.menu.top.configuration.repositoryObjects"),
				PageDebugList.class);
		submenu.add(menu);
		menu = new MenuItem(createStringResource("PageAdmin.menu.top.configuration.repositoryObjectView"),
				PageDebugView.class, null, createVisibleDisabledBehaviorForEditMenu(PageDebugView.class));
		submenu.add(menu);
		if (SystemConfigurationHolder.isExperimentalCodeEnabled()) {
			menu = new MenuItem(createStringResource("PageAdmin.menu.top.configuration.repoQuery"),
					PageRepoQuery.class);
			submenu.add(menu);
		}

		PageParameters params = new PageParameters();
		params.add(PageSystemConfiguration.SELECTED_TAB_INDEX, PageSystemConfiguration.CONFIGURATION_TAB_BASIC);
		menu = new MenuItem(createStringResource("PageAdmin.menu.top.configuration.basic"),
				PageSystemConfiguration.class, params, null) {

			@Override
			public boolean isMenuActive(WebPage page) {
				if (!PageSystemConfiguration.class.equals(page.getClass())) {
					return false;
				}

				int index = getSelectedTabForConfiguration(page);
				return PageSystemConfiguration.CONFIGURATION_TAB_BASIC == index ? true : false;
			}
		};
		submenu.add(menu);

		params = new PageParameters();
		params.add(PageSystemConfiguration.SELECTED_TAB_INDEX, PageSystemConfiguration.CONFIGURATION_TAB_NOTIFICATION);
		menu = new MenuItem(createStringResource("PageAdmin.menu.top.configuration.notifications"),
				PageSystemConfiguration.class, params, null) {

			@Override
			public boolean isMenuActive(WebPage page) {
				if (!PageSystemConfiguration.class.equals(page.getClass())) {
					return false;
				}

				int index = getSelectedTabForConfiguration(page);
				return PageSystemConfiguration.CONFIGURATION_TAB_NOTIFICATION == index ? true : false;
			}
		};
		submenu.add(menu);

		params = new PageParameters();
		params.add(PageSystemConfiguration.SELECTED_TAB_INDEX, PageSystemConfiguration.CONFIGURATION_TAB_LOGGING);
		menu = new MenuItem(createStringResource("PageAdmin.menu.top.configuration.logging"),
				PageSystemConfiguration.class, params, null) {

			@Override
			public boolean isMenuActive(WebPage page) {
				if (!PageSystemConfiguration.class.equals(page.getClass())) {
					return false;
				}

				int index = getSelectedTabForConfiguration(page);
				return PageSystemConfiguration.CONFIGURATION_TAB_LOGGING == index ? true : false;
			}
		};
		submenu.add(menu);

		params = new PageParameters();
		params.add(PageSystemConfiguration.SELECTED_TAB_INDEX, PageSystemConfiguration.CONFIGURATION_TAB_PROFILING);
		menu = new MenuItem(createStringResource("PageAdmin.menu.top.configuration.profiling"),
				PageSystemConfiguration.class, params, null) {

			@Override
			public boolean isMenuActive(WebPage page) {
				if (!PageSystemConfiguration.class.equals(page.getClass())) {
					return false;
				}

				int index = getSelectedTabForConfiguration(page);
				return PageSystemConfiguration.CONFIGURATION_TAB_PROFILING == index ? true : false;
			}
		};
		submenu.add(menu);

		params = new PageParameters();
		params.add(PageSystemConfiguration.SELECTED_TAB_INDEX, PageSystemConfiguration.CONFIGURATION_TAB_ADMIN_GUI);
		menu = new MenuItem(createStringResource("PageAdmin.menu.top.configuration.adminGui"),
				PageSystemConfiguration.class, params, null) {

			@Override
			public boolean isMenuActive(WebPage page) {
				if (!PageSystemConfiguration.class.equals(page.getClass())) {
					return false;
				}

				int index = getSelectedTabForConfiguration(page);
				return PageSystemConfiguration.CONFIGURATION_TAB_ADMIN_GUI == index ? true : false;
			}
		};
		submenu.add(menu);

		menu = new MenuItem(createStringResource("PageAdmin.menu.top.configuration.shadowsDetails"),
				PageAccounts.class);
		submenu.add(menu);
		menu = new MenuItem(createStringResource("PageAdmin.menu.top.configuration.internals"), PageInternals.class);
		submenu.add(menu);

		menu = new MenuItem(createStringResource("PageAdmin.menu.top.configuration.about"), PageAbout.class);
		submenu.add(menu);

		return item;
	}

	private int getSelectedTabForConfiguration(WebPage page) {
		PageParameters params = page.getPageParameters();
		StringValue val = params.get(PageSystemConfiguration.SELECTED_TAB_INDEX);
		String value = null;
		if (val != null && !val.isNull()) {
			value = val.toString();
		}

		return StringUtils.isNumeric(value) ? Integer.parseInt(value) : PageSystemConfiguration.CONFIGURATION_TAB_BASIC;
	}

	private void createSelfServiceMenu(SideBarMenuItem menu) {
		MainMenuItem item = new MainMenuItem("fa fa-dashboard", createStringResource("PageAdmin.menu.selfDashboard"),
				PageSelfDashboard.class);
		menu.getItems().add(item);
		item = new MainMenuItem("fa fa-user", createStringResource("PageAdmin.menu.profile"), PageSelfProfile.class);
		menu.getItems().add(item);
		// PageSelfAssignments is not implemented yet
		// item = new MainMenuItem("fa fa-star",
		// createStringResource("PageAdmin.menu.assignments"),
		// PageSelfAssignments.class);
		// menu.getItems().add(item);
		item = new MainMenuItem("fa fa-shield", createStringResource("PageAdmin.menu.credentials"),
				PageSelfCredentials.class);
		menu.getItems().add(item);
	}

    private void createAdditionalMenu(SideBarMenuItem menu) {
        AdminGuiConfigurationType adminGuiConfig = loadAdminGuiConfiguration();
        if (adminGuiConfig != null) {
            List<RichHyperlinkType> menuList = loadAdminGuiConfiguration().getAdditionalMenuLink();

            Map<String, Class> urlClassMap = DescriptorLoader.getUrlClassMap();
            if (menuList != null && menuList.size() > 0 && urlClassMap != null && urlClassMap.size() > 0) {
                for (RichHyperlinkType link : menuList) {
                    if (link.getTargetUrl() != null && !link.getTargetUrl().trim().equals("")) {
                        MainMenuItem item = new MainMenuItem(link.getIcon() == null ? "" : link.getIcon().getCssClass(),
                                getAdditionalMenuItemNameModel(link.getLabel()),
                                urlClassMap.get(link.getTargetUrl()));
                        menu.getItems().add(item);
                    }
                }
            }
        }
    }

    private IModel<String> getAdditionalMenuItemNameModel(final String name){
        return new IModel<String>() {
            @Override
            public String getObject() {
                return name;
            }

            @Override
            public void setObject(String s) {
            }

            @Override
            public void detach() {
            }
        };
    }

    private MainMenuItem createHomeItems() {
		MainMenuItem item = new MainMenuItem("fa fa-dashboard", createStringResource("PageAdmin.menu.dashboard"),
				PageDashboard.class);

		return item;
	}

	private MainMenuItem createUsersItems() {
		MainMenuItem item = new MainMenuItem("fa fa-group", createStringResource("PageAdmin.menu.top.users"), null);

		List<MenuItem> submenu = item.getItems();

		MenuItem list = new MenuItem(createStringResource("PageAdmin.menu.top.users.list"), PageUsers.class);
		submenu.add(list);
		createFocusPageNewEditMenu(submenu, "PageAdmin.menu.top.users.new", "PageAdmin.menu.top.users.edit",
				PageUser.class);
		// MenuItem search = new
		// MenuItem(createStringResource("PageAdmin.menu.users.search"),
		// PageUsersSearch.class);
		// submenu.add(search);

		return item;
	}

	private void createFocusPageNewEditMenu(List<MenuItem> submenu, String newKey, String editKey,
			final Class<? extends PageAdmin> newPageType) {
		MenuItem edit = new MenuItem(createStringResource(editKey), newPageType, null, new VisibleEnableBehaviour() {

			@Override
			public boolean isEnabled() {
				return false;
			}

			@Override
			public boolean isVisible() {
				if (!getPage().getClass().equals(newPageType)) {
					return false;
				}

				if (getPage() instanceof PageAdminFocus) {
					PageAdminFocus page = (PageAdminFocus) getPage();
					return page.isEditingFocus();
				} else if (getPage() instanceof PageResourceWizard) {
					PageResourceWizard page = (PageResourceWizard) getPage();
					return !page.isNewResource();
				} else {
					return false;
				}
			}
		});
		submenu.add(edit);
		MenuItem newMenu = new MenuItem(createStringResource(newKey), newPageType) {

			@Override
			protected boolean isMenuActive() {
				if (!PageBase.this.getPage().getClass().equals(newPageType)) {
					return false;
				}

				if (PageBase.this.getPage() instanceof PageAdminFocus) {
					PageAdminFocus page = (PageAdminFocus) PageBase.this.getPage();
					return !page.isEditingFocus();
				} else if (PageBase.this.getPage() instanceof PageResourceWizard) {
					PageResourceWizard page = (PageResourceWizard) PageBase.this.getPage();
					return page.isNewResource();
				} else {
					return false;
				}
			}
		};
		submenu.add(newMenu);
	}

	private void createFocusPageViewMenu(List<MenuItem> submenu, String viewKey,
			final Class<? extends PageBase> newPageType) {
		MenuItem view = new MenuItem(createStringResource(viewKey), newPageType, null, new VisibleEnableBehaviour() {

			@Override
			public boolean isEnabled() {
				return false;
			}

			@Override
			public boolean isVisible() {
				if (!getPage().getClass().equals(newPageType)) {
					return false;
				}

				return true;
			}
		});
		submenu.add(view);
	}

	private MainMenuItem createOrganizationsMenu() {
		MainMenuItem item = new MainMenuItem("fa fa-building", createStringResource("PageAdmin.menu.top.users.org"),
				null);

		List<MenuItem> submenu = item.getItems();

		MenuItem list = new MenuItem(createStringResource("PageAdmin.menu.top.users.org.tree"), PageOrgTree.class);
		submenu.add(list);
		createFocusPageNewEditMenu(submenu, "PageAdmin.menu.top.users.org.new", "PageAdmin.menu.top.users.org.edit",
				PageOrgUnit.class);

		return item;
	}

	private MainMenuItem createRolesItems() {
		MainMenuItem item = new MainMenuItem("fa fa-street-view", createStringResource("PageAdmin.menu.top.roles"),
				null);

		List<MenuItem> submenu = item.getItems();

		MenuItem list = new MenuItem(createStringResource("PageAdmin.menu.top.roles.list"), PageRoles.class);
		submenu.add(list);
		createFocusPageNewEditMenu(submenu, "PageAdmin.menu.top.roles.new", "PageAdmin.menu.top.roles.edit",
				PageRole.class);

		return item;
	}

	protected PrismObject<UserType> loadUserSelf(PageBase page) {
		Task task = createSimpleTask(OPERATION_LOAD_USER);
		OperationResult result = task.getResult();
		PrismObject<UserType> user = WebModelServiceUtils.loadObject(UserType.class,
				WebModelServiceUtils.getLoggedInUserOid(), page, task, result);
		result.computeStatus();

		showResult(result, null, false);

		return user;
	}

	private VisibleEnableBehaviour createVisibleDisabledBehaviorForEditMenu(final Class<? extends WebPage> page) {
		return new VisibleEnableBehaviour() {

			@Override
			public boolean isVisible() {
				return getPage().getClass().equals(page);
			}

			@Override
			public boolean isEnabled() {
				return false;
			}
		};
	}


    public AdminGuiConfigurationType loadAdminGuiConfiguration() {
        MidPointPrincipal user = SecurityUtils.getPrincipalUser();
        AdminGuiConfigurationType adminGuiConfig = null;
        if (user == null) {
            return adminGuiConfig;
        } else {
            OperationResult result = new OperationResult(OPERATION_GET_SYSTEM_CONFIG);
            Task task = createSimpleTask(OPERATION_GET_SYSTEM_CONFIG);
            try {
                adminGuiConfig = getModelInteractionService().getAdminGuiConfiguration(task, result);
                LOGGER.trace("Admin GUI config: {}", adminGuiConfig);
                result.recordSuccess();
            } catch(Exception ex){
                LoggingUtils.logException(LOGGER, "Couldn't load system configuration", ex);
                result.recordFatalError("Couldn't load system configuration.", ex);
            }
            return adminGuiConfig;
        }
    }

	public void redirectBack() {
		List<Breadcrumb> breadcrumbs = getSessionStorage().getBreadcrumbs();
		if (breadcrumbs.size() < 2) {
			return;
		}

		Breadcrumb breadcrumb = breadcrumbs.get(breadcrumbs.size() - 2);
		redirectBackToBreadcrumb(breadcrumb);
	}

	public void redirectBackToBreadcrumb(Breadcrumb breadcrumb) {
		Validate.notNull(breadcrumb, "Breadcrumb must not be null");

		boolean found = false;

		//we remove all breadcrumbs that are after "breadcrumb"
		List<Breadcrumb> breadcrumbs = getSessionStorage().getBreadcrumbs();
		Iterator<Breadcrumb> iterator = breadcrumbs.iterator();
		while (iterator.hasNext()) {
			Breadcrumb b = iterator.next();
			if (b.equals(breadcrumb)) {
				found = true;
			}

			if (found) {
				iterator.remove();
			}
		}

		breadcrumb.redirect(this);
	}
}
