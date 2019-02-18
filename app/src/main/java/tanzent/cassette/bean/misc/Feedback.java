package tanzent.cassette.bean.misc;

/**
 * Created by Remix on 2016/12/1.
 */

public class Feedback {
    public String Content;
    public String AppVersion;
    public String AppVersionCode;
    public String CpuABI;
    public String DeviceManufacturer;
    public String DeviceModel;
    public String ReleaseVersion;
    public String SdkVersion;
    public String Display;
    public String Contact;

    public Feedback(String content, String contact, String appVersion, String appVersionCode, String display, String cpuABI, String deviceManufacturer, String deviceModel, String releaseVersion, String sdkVersion) {
        super();
        Content = content;
        Contact = contact;
        AppVersion = appVersion;
        AppVersionCode = appVersionCode;
        Display = display;
        CpuABI = cpuABI;
        DeviceManufacturer = deviceManufacturer;
        DeviceModel = deviceModel;
        ReleaseVersion = releaseVersion;
        SdkVersion = sdkVersion;
    }

    public Feedback() {
    }

    @Override
    public String toString() {
        return "AppVersion: " + AppVersion + '\n' +
                "AppVersionCode: " + AppVersionCode + '\n' +
                "CpuABI: " + CpuABI + '\n' +
                "DeviceManufacturer: " + DeviceManufacturer + '\n' +
                "DeviceModel: " + DeviceModel + '\n' +
                "ReleaseVersion: " + ReleaseVersion + '\n' +
                "SdkVersion= : " + SdkVersion + '\n' +
                "Display: " + Display + '\n' +
                "Contact: " + Contact + '\n';
    }

    public String getContent() {
        return Content;
    }

    public void setContent(String content) {
        Content = content;
    }

    public String getContact() {
        return Contact;
    }

    public void setContact(String contact) {
        Contact = contact;
    }

    public String getAppVersion() {
        return AppVersion;
    }

    public void setAppVersion(String appVersion) {
        AppVersion = appVersion;
    }

    public String getAppVersionCode() {
        return AppVersionCode;
    }

    public String getDisplay() {
        return Display;
    }

    public void setDisplay(String display) {
        Display = display;
    }

    public void setAppVersionCode(String appVersionCode) {
        AppVersionCode = appVersionCode;
    }

    public String getCpuABI() {
        return CpuABI;
    }

    public void setCpuABI(String cpuABI) {
        CpuABI = cpuABI;
    }

    public String getDeviceManufacturer() {
        return DeviceManufacturer;
    }

    public void setDeviceManufacturer(String deviceManufacturer) {
        DeviceManufacturer = deviceManufacturer;
    }

    public String getDeviceModel() {
        return DeviceModel;
    }

    public void setDeviceModel(String deviceModel) {
        DeviceModel = deviceModel;
    }

    public String getReleaseVersion() {
        return ReleaseVersion;
    }

    public void setReleaseVersion(String releaseVersion) {
        ReleaseVersion = releaseVersion;
    }

    public String getSdkVersion() {
        return SdkVersion;
    }

    public void setSdkVersion(String sdkVersion) {
        SdkVersion = sdkVersion;
    }
}
