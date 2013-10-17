package eu.trentorise.smartcampus.dt.model;

import eu.trentorise.smartcampus.storage.BasicObject;
import eu.trentorise.smartcampus.territoryservice.model.BaseDTObject;
import eu.trentorise.smartcampus.territoryservice.model.POIObject;

public class GenericObjectForBean extends BasicObject {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7872581890857683931L;
	protected BaseDTObject objectForBean;

	@Override
	public void setId(String id) {
		objectForBean.setId(id);
	}

	@Override
	public void setUpdateTime(long updateTime) {
		objectForBean.setUpdateTime(updateTime);
	}

	@Override
	public void setVersion(long version) {
		objectForBean.setVersion(version);
	}

	public void setObjectForBean(BaseDTObject objectForBean) {
		this.objectForBean = objectForBean;
	}

	@Override
	public String getId() {
		return objectForBean.getId();
	}

	@Override
	public long getUpdateTime() {
		return objectForBean.getUpdateTime();
	}

	@Override
	public long getVersion() {
		return objectForBean.getVersion();
	}

	public BaseDTObject getObjectForBean() {
		return objectForBean;
	}
}
