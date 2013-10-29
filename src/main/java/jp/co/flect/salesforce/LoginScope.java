package jp.co.flect.salesforce;

public class LoginScope {
	
	private String orgId;
	private String portalId;
	
	public LoginScope(String orgId, String portalId) {
		this.orgId = orgId;
		this.portalId = portalId;
	}
	
	public String getOrganizationId() { return this.orgId;}
	public String getPortalId() { return this.portalId;}
}
