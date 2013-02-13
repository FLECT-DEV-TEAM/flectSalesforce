package jp.co.flect.salesforce.metadata;

public enum MetadataType {
	//TopLevel
	AnalyticSnapshot,
	ApexClass,
	ApexComponent,
	ApexPage,
	ApexTrigger,
	ArticleType,
	BaseSharingRule,
	CriteriaBasedSharingRule,
	CustomApplication,
	CustomApplicationComponent,
	CustomLabels,
	CustomObject,
	CustomObjectTranslation,
	CustomPageWebLink,
	CustomSite,
	CustomTab,
	Dashboard,
	DataCategoryGroup,
	Document,
	EmailTemplate,
	EntitlementTemplate,
	Flow,
	Folder,
	Group,
	HomePageComponent,
	HomePageLayout,
	Layout,
	Letterhead,
	Metadata,
	MetadataWithContent,
	OwnerSharingRule,
	Package,
	PermissionSet,
	Portal,
	Profile,
	Queue,
	RemoteSiteSetting,
	Report,
	ReportType,
	Role,
	Scontrol,
	SecuritySettings,
	SharingRules,
	StaticResource,
	Territory,
	Translations,
	Weblink,
	Workflow,
	//
	CustomField(false),
	PicklistValue(false)
	;
	
	private boolean topLevel;
	
	private MetadataType() {
		this(true);
	}
	
	private MetadataType(boolean topLevel) {
		this.topLevel = topLevel;
	}
	
	public boolean isTopLevel() { return this.topLevel;}
	
	public static MetadataType getTargetType(String str) {
		for (MetadataType mt : MetadataType.values()) {
			String name = mt.name();
			if (name.equalsIgnoreCase(str)) {
				return mt;
			}
			if (name.startsWith("Custom") && name.substring(6).equalsIgnoreCase(str)) {
				return mt;
			}
		}
		return null;
	}
}

