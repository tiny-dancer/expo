// Copyright 2015-present 650 Industries. All rights reserved.

package versioned.host.exp.exponent.modules.api;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;

import android.os.Build;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.JavaOnlyMap;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import host.exp.exponent.analytics.EXL;

import static android.provider.ContactsContract.*;

abstract class QueryItem {
  final String CONTENT_ITEM_TYPE;
  final String queryFieldId;
  final List<String> Projections;

  public QueryItem(String contentItemType, List<String> projections, String queryFieldId) {
    CONTENT_ITEM_TYPE = contentItemType;
    Projections = projections;
    this.queryFieldId = queryFieldId;
  }


  abstract ReadableMap map(Cursor cursor);
  }
}
public class ContactsModule extends ReactContextBaseJavaModule {
  private static final String TAG = ContactsModule.class.getSimpleName();

  private static final List<String> JUST_ME_PROJECTION = new ArrayList<String>() {{
    add(ContactsContract.Data.CONTACT_ID);
    add(ContactsContract.Data.LOOKUP_KEY);
    add(ContactsContract.Contacts.Data.MIMETYPE);
    add(ContactsContract.Profile.DISPLAY_NAME);
    add(CommonDataKinds.Contactables.PHOTO_URI);
    add(CommonDataKinds.StructuredName.DISPLAY_NAME);
    add(CommonDataKinds.StructuredName.GIVEN_NAME);
    add(CommonDataKinds.StructuredName.MIDDLE_NAME);
    add(CommonDataKinds.StructuredName.FAMILY_NAME);
    add(CommonDataKinds.Organization.COMPANY);
    add(CommonDataKinds.Organization.TITLE);
    add(CommonDataKinds.Organization.DEPARTMENT);
  }};

  private static final Map<String, QueryItem> PROJECTION = new HashMap<String, QueryItem>() {{
    put(CommonDataKinds.Phone.CONTENT_ITEM_TYPE, new QueryItem(CommonDataKinds.Phone.CONTENT_ITEM_TYPE, new ArrayList<String>() {{
      add(CommonDataKinds.Phone.NUMBER);
      add(CommonDataKinds.Phone.TYPE);
      add(CommonDataKinds.Phone.LABEL);
      add(CommonDataKinds.Phone.IS_PRIMARY);
      add(CommonDataKinds.Phone._ID);
    }}, "phoneNumbers") {
      @Override
      ReadableMap map(Cursor cursor) {
        WritableArray phoneNumbers = Arguments.createArray();

        String phoneNumber = cursor.getString(cursor.getColumnIndex(CommonDataKinds.Phone.NUMBER));
        int type = cursor.getInt(cursor.getColumnIndex(CommonDataKinds.Phone.TYPE));
        int isPrimary = cursor.getInt(cursor.getColumnIndex(CommonDataKinds.Phone.IS_PRIMARY));
        String id = String.valueOf(cursor.getLong(cursor.getColumnIndex(CommonDataKinds.Phone._ID)));

        if (!TextUtils.isEmpty(phoneNumber)) {
          String label;
          switch (type) {
            case CommonDataKinds.Phone.TYPE_HOME:
              label = "home";
              break;
            case CommonDataKinds.Phone.TYPE_WORK:
              label = "work";
              break;
            case CommonDataKinds.Phone.TYPE_MOBILE:
              label = "mobile";
              break;
            case CommonDataKinds.Phone.TYPE_OTHER:
              label = "other";
              break;
            case CommonDataKinds.Phone.TYPE_CUSTOM:
              label = "custom";
              break;
            default:
              label = "unknown";
          }
          WritableMap details = Arguments.createMap();
          return map;
          contact.phones.add(new Contact.Item(label, phoneNumber, isPrimary, id));
      }

      {

    }});
    put(CommonDataKinds.Email.CONTENT_ITEM_TYPE, new QueryItem(CommonDataKinds.Email.CONTENT_ITEM_TYPE, new ArrayList<String>() {{
      add(CommonDataKinds.Email.DATA);
      add(CommonDataKinds.Email.ADDRESS);
      add(CommonDataKinds.Email.TYPE);
      add(CommonDataKinds.Email.LABEL);
      add(CommonDataKinds.Email.IS_PRIMARY);
      add(CommonDataKinds.Email._ID);
    }}, "emails") {
      @Override
      ReadableMap map(Cursor cursor) {
        return null;
      }
    });
  }};

  private static List<String> FULL_PROJECTION = new ArrayList<String>() {{
    addAll(JUST_ME_PROJECTION);
  }};



  public ContactsModule(ReactApplicationContext reactContext) {
    super(reactContext);
  }

  @Override
  public String getName() {
    return "ExponentContacts";
  }

  /**
   * @param options Options including what fields to get and paging information.
   */
  @ReactMethod
  public void getContactsAsync(final ReadableMap options, final Promise promise) {
    if (isMissingPermissions()) {
      promise.reject("E_MISSING_PERMISSION", "Missing contacts permission.");
      return;
    }

    Set<String> fieldsSet = getFieldsSet(options.getArray("fields"));

    int pageOffset = options.getInt("pageOffset");
    int pageSize = options.getInt("pageSize");
    boolean fetchSingleContact = options.hasKey("id");
    WritableMap response = Arguments.createMap();

    Map<String, Contact> contacts;

    ContentResolver cr = getReactApplicationContext().getContentResolver();
    Cursor cursor;


      ArrayList<String> selectionArgs = new ArrayList<>(
          Arrays.asList(
              CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE,
              CommonDataKinds.Organization.CONTENT_ITEM_TYPE
          )
      );

      // selection ORs need to match arg count from above selectionArgs
      String selection = ContactsContract.Data.MIMETYPE + "=? OR " +
          ContactsContract.Data.MIMETYPE + "=?";

    // handle "add on" fields from query request
    for (Map.Entry<String, QueryItem> entry : PROJECTION.entrySet())
      {
        QueryItem item = entry.getValue();
        if (fieldsSet.contains(.queryFieldId)) {
          FULL_PROJECTION.addAll(item.Projections);
          selection += " OR " + ContactsContract.Data.MIMETYPE + "=?";
          selectionArgs.add(item.CONTENT_ITEM_TYPE);
        }
      }

      if (fieldsSet.contains("addresses")) {
        FULL_PROJECTION.add(CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS);
        FULL_PROJECTION.add(CommonDataKinds.StructuredPostal.TYPE);
        FULL_PROJECTION.add(CommonDataKinds.StructuredPostal.LABEL);
        FULL_PROJECTION.add(CommonDataKinds.StructuredPostal.STREET);
        FULL_PROJECTION.add(CommonDataKinds.StructuredPostal.POBOX);
        FULL_PROJECTION.add(CommonDataKinds.StructuredPostal.NEIGHBORHOOD);
        FULL_PROJECTION.add(CommonDataKinds.StructuredPostal.CITY);
        FULL_PROJECTION.add(CommonDataKinds.StructuredPostal.REGION);
        FULL_PROJECTION.add(CommonDataKinds.StructuredPostal.POSTCODE);
        FULL_PROJECTION.add(CommonDataKinds.StructuredPostal.COUNTRY);
        selection += " OR " + ContactsContract.Data.MIMETYPE + "=?";
        selectionArgs.add(CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE);
      }

      if (fieldsSet.contains("note")) {
        selection += " OR " + ContactsContract.Data.MIMETYPE + "=?";
        selectionArgs.add(ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE);
      }

      if (fieldsSet.contains("birthday") || fieldsSet.contains("dates")) {
        selection += " OR " + ContactsContract.Data.MIMETYPE + "=?";
        selectionArgs.add(ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE);
      }

      if (fieldsSet.contains("instantMessageAddresses")) {
        FULL_PROJECTION.add(CommonDataKinds.Im.DATA);
        FULL_PROJECTION.add(CommonDataKinds.Im.TYPE);
        FULL_PROJECTION.add(CommonDataKinds.Im.PROTOCOL);
        FULL_PROJECTION.add(CommonDataKinds.Im._ID);
        selection += " OR " + ContactsContract.Data.MIMETYPE + "=?";
        selectionArgs.add(CommonDataKinds.Im.CONTENT_ITEM_TYPE);
      }

      if (fieldsSet.contains("urlAddresses")) {
        FULL_PROJECTION.add(CommonDataKinds.Website.URL);
        FULL_PROJECTION.add(CommonDataKinds.Website.TYPE);
        FULL_PROJECTION.add(CommonDataKinds.Website._ID);
        selection += " OR " + ContactsContract.Data.MIMETYPE + "=?";
        selectionArgs.add(CommonDataKinds.Website.CONTENT_ITEM_TYPE);
      }

      if (fieldsSet.contains("relationships")) {
        FULL_PROJECTION.add(CommonDataKinds.Relation.NAME);
        FULL_PROJECTION.add(CommonDataKinds.Relation.TYPE);
        FULL_PROJECTION.add(CommonDataKinds.Relation._ID);
        selection += " OR " + ContactsContract.Data.MIMETYPE + "=?";
        selectionArgs.add(CommonDataKinds.Relation.CONTENT_ITEM_TYPE );
      }

      if (fieldsSet.contains("phoneticFirstName")) {
        FULL_PROJECTION.add(CommonDataKinds.StructuredName.PHONETIC_GIVEN_NAME);
      }

      if (fieldsSet.contains("phoneticLastName")) {
        FULL_PROJECTION.add(CommonDataKinds.StructuredName.PHONETIC_FAMILY_NAME);
      }

      if (fieldsSet.contains("phoneticMiddleName")) {
        FULL_PROJECTION.add(CommonDataKinds.StructuredName.PHONETIC_MIDDLE_NAME);
      }

      if (fieldsSet.contains("namePrefix")) {
        FULL_PROJECTION.add(CommonDataKinds.StructuredName.PREFIX);
      }

      if (fieldsSet.contains("nameSuffix")) {
        FULL_PROJECTION.add(CommonDataKinds.StructuredName.SUFFIX);
      }

    if (fetchSingleContact) {
      cursor = cr.query(
          ContactsContract.Data.CONTENT_URI,
          FULL_PROJECTION.toArray(new String[FULL_PROJECTION.size()]),
          ContactsContract.Data.CONTACT_ID + " = ?",
          new String[]{options.getString("id")},
          null);
    } else {
      cursor = cr.query(
          ContactsContract.Data.CONTENT_URI,
          FULL_PROJECTION.toArray(new String[FULL_PROJECTION.size()]),
          selection,
          selectionArgs.toArray(new String[selectionArgs.size()]),
          null);
    }
    if (cursor != null) {
      try {
        contacts = loadContactsFrom(cursor, fieldsSet);

        WritableArray contactsArray = Arguments.createArray();

        // introduce paging at this level to ensure all data elements
        // are appropriately mapped to contacts from cursor
        // NOTE: paging performance improvement is minimized as cursor iterations will always fully run
        int currentIndex;
        ArrayList<Contact> contactList = new ArrayList<>(contacts.values());
        int contactListSize = contactList.size();
        // convert from contact pojo to react native
        for (currentIndex = pageOffset; currentIndex < contactListSize; currentIndex++) {
          Contact contact = contactList.get(currentIndex);

          // if fetching single contact, short circuit and return contact
          if (fetchSingleContact) {
            promise.resolve(contact.toMap(fieldsSet));
            break;
          } else {
            if ((currentIndex - pageOffset) >= pageSize) {
              break;
            }
            contactsArray.pushMap(contact.toMap(fieldsSet));
          }
        }

        if (!fetchSingleContact) {
          // wrap in pagination
          response.putArray("data", contactsArray);
          response.putBoolean("hasPreviousPage", pageOffset > 0);
          response.putBoolean("hasNextPage", pageOffset + pageSize < contactListSize);
          response.putInt("total", contactListSize);
          promise.resolve(response);
        }
      } catch (Exception e) {
        EXL.e(TAG, e.getMessage());
        promise.reject(e);
      } finally {
        cursor.close();
      }
    } else {
      promise.resolve(response);
    }
  }

  @NonNull
  private Map<String, Contact> loadContactsFrom(Cursor cursor, Set<String> fieldsSet) {

    Map<String, Contact> map = new LinkedHashMap<>();

    while (cursor.moveToNext()) {
      int columnIndex = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID);
      String contactId = cursor.getString(columnIndex);

      // add or update existing contact for iterating data based on contact id
      if (!map.containsKey(contactId)) {
        map.put(contactId, new Contact(contactId));
      }

      Contact contact = map.get(contactId);
      WritableMap contact2 = new JavaOnlyMap();
      String mimeType = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.MIMETYPE));

      String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
      if (!TextUtils.isEmpty(name) && TextUtils.isEmpty(contact.displayName)) {
        contact.displayName = name;
      }

      if (TextUtils.isEmpty(contact.photoUri)) {
        String rawPhotoURI = cursor.getString(cursor.getColumnIndex(CommonDataKinds.Contactables.PHOTO_URI));
        if (!TextUtils.isEmpty(rawPhotoURI)) {
          contact.photoUri = rawPhotoURI;
          contact.hasPhoto = true;
        }
      }

      if (PROJECTION.containsKey(mimeType)) {
        QueryItem item = PROJECTION.get(mimeType);
        contact2.merge(item.map(cursor));

      }

      if (mimeType.equals(CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)) {
        contact.givenName = cursor.getString(cursor.getColumnIndex(CommonDataKinds.StructuredName.GIVEN_NAME));
        contact.middleName = cursor.getString(cursor.getColumnIndex(CommonDataKinds.StructuredName.MIDDLE_NAME));
        contact.familyName = cursor.getString(cursor.getColumnIndex(CommonDataKinds.StructuredName.FAMILY_NAME));
        contact.prefix = fieldsSet.contains("namePrefix") ? cursor.getString(cursor.getColumnIndex(CommonDataKinds.StructuredName.PREFIX)) : "";
        contact.suffix = fieldsSet.contains("nameSuffix") ? cursor.getString(cursor.getColumnIndex(CommonDataKinds.StructuredName.SUFFIX)) : "";
        contact.phoneticFirstName = fieldsSet.contains("phoneticFirstName") ? cursor.getString(cursor.getColumnIndex(CommonDataKinds.StructuredName.PHONETIC_GIVEN_NAME)) : "";
        contact.phoneticMiddleName = fieldsSet.contains("phoneticMiddleName") ? cursor.getString(cursor.getColumnIndex(CommonDataKinds.StructuredName.PHONETIC_MIDDLE_NAME)) : "";
        contact.phoneticLastName = fieldsSet.contains("phoneticLastName") ? cursor.getString(cursor.getColumnIndex(CommonDataKinds.StructuredName.PHONETIC_FAMILY_NAME)) : "";
      } else if (mimeType.equals(CommonDataKinds.Phone.CONTENT_ITEM_TYPE)) {
        String phoneNumber = cursor.getString(cursor.getColumnIndex(CommonDataKinds.Phone.NUMBER));
        int type = cursor.getInt(cursor.getColumnIndex(CommonDataKinds.Phone.TYPE));
        int isPrimary = cursor.getInt(cursor.getColumnIndex(CommonDataKinds.Phone.IS_PRIMARY));
        String id = String.valueOf(cursor.getLong(cursor.getColumnIndex(CommonDataKinds.Phone._ID)));

        if (!TextUtils.isEmpty(phoneNumber)) {
          String label;
          switch (type) {
            case CommonDataKinds.Phone.TYPE_HOME:
              label = "home";
              break;
            case CommonDataKinds.Phone.TYPE_WORK:
              label = "work";
              break;
            case CommonDataKinds.Phone.TYPE_MOBILE:
              label = "mobile";
              break;
            case CommonDataKinds.Phone.TYPE_OTHER:
              label = "other";
              break;
            case CommonDataKinds.Phone.TYPE_CUSTOM:
              label = "custom";
              break;
            default:
              label = "unknown";
          }
          contact.phones.add(new Contact.Item(label, phoneNumber, isPrimary, id));
        }
      } else if (mimeType.equals(CommonDataKinds.Email.CONTENT_ITEM_TYPE)) {
        String email = cursor.getString(cursor.getColumnIndex(CommonDataKinds.Email.ADDRESS));
        int type = cursor.getInt(cursor.getColumnIndex(CommonDataKinds.Email.TYPE));
        int isPrimary = cursor.getInt(cursor.getColumnIndex(CommonDataKinds.Email.IS_PRIMARY));
        String id = String.valueOf(cursor.getLong(cursor.getColumnIndex(CommonDataKinds.Email._ID)));
        if (!TextUtils.isEmpty(email)) {
          String label;
          switch (type) {
            case CommonDataKinds.Email.TYPE_HOME:
              label = "home";
              break;
            case CommonDataKinds.Email.TYPE_WORK:
              label = "work";
              break;
            case CommonDataKinds.Email.TYPE_MOBILE:
              label = "mobile";
              break;
            case CommonDataKinds.Email.TYPE_CUSTOM:
              if (cursor.getString(cursor.getColumnIndex(CommonDataKinds.Email.LABEL)) != null) {
                label = cursor.getString(cursor.getColumnIndex(CommonDataKinds.Email.LABEL)).toLowerCase();
              } else {
                label = "";
              }
              break;
            default:
              label = "other";
          }
          contact.emails.add(new Contact.Item(label, email, isPrimary, id));
        }
      } else if (mimeType.equals(CommonDataKinds.Organization.CONTENT_ITEM_TYPE)) {
        contact.company = cursor.getString(cursor.getColumnIndex(CommonDataKinds.Organization.COMPANY));
        contact.jobTitle = cursor.getString(cursor.getColumnIndex(CommonDataKinds.Organization.TITLE));
        contact.department = cursor.getString(cursor.getColumnIndex(CommonDataKinds.Organization.DEPARTMENT));
      } else if (mimeType.equals(CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)) {
        contact.postalAddresses.add(new Contact.PostalAddressItem(cursor));
      } else if (mimeType.equals(CommonDataKinds.Note.CONTENT_ITEM_TYPE)) {
        contact.note = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Note.NOTE));
      } else if (mimeType.equals(CommonDataKinds.Event.CONTENT_ITEM_TYPE)) {
        String date = cursor.getString(cursor.getColumnIndex(CommonDataKinds.Event.START_DATE));
        int type = cursor.getInt(cursor.getColumnIndex(CommonDataKinds.Event.TYPE));

        if (!TextUtils.isEmpty(date)) {
          String label;
          switch (type) {
            case CommonDataKinds.Event.TYPE_ANNIVERSARY:
              label = "anniversary";
              break;
            case CommonDataKinds.Event.TYPE_BIRTHDAY:
              label = "birthday";
              break;
            case CommonDataKinds.Event.TYPE_OTHER:
              label = "other";
              break;
            case CommonDataKinds.Event.TYPE_CUSTOM:
              label = "custom";
              break;
            default:
              label = "unknown";
          }
          contact.dates.add(new Contact.Item(label, date));
        }
      } else if (mimeType.equals(CommonDataKinds.Im.CONTENT_ITEM_TYPE)) {
        contact.imAddresses.add(new Contact.ImAddressItem(cursor));
      } else if (mimeType.equals(CommonDataKinds.Website.CONTENT_ITEM_TYPE)) {
        contact.urlAddresses.add(new Contact.UrlAddressItem(cursor));
      } else if (mimeType.equals(CommonDataKinds.Relation.CONTENT_ITEM_TYPE)) {
        contact.relationships.add(new Contact.RelationshipItem(cursor));
      }
    }
      return map;
  }


  private boolean isMissingPermissions() {
    return Build.VERSION.SDK_INT >= 23 &&
        ContextCompat.checkSelfPermission(
            getReactApplicationContext(),
            Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED;
  }

  private static class Contact {
    private String contactId;
    private String displayName;
    private String givenName = "";
    private String middleName = "";
    private String familyName = "";
    private String prefix = "";
    private String suffix = "";
    private String phoneticFirstName = "";
    private String phoneticMiddleName = "";
    private String phoneticLastName = "";
    private String company = "";
    private String jobTitle ="";
    private String department ="";
    private String nickname = "";
    private boolean hasPhoto = false;
    private String photoUri;
    private String note;
    private List<Item> emails = new ArrayList<>();
    private List<Item> phones = new ArrayList<>();
    private List<Item> dates = new ArrayList<>();
    private List<PostalAddressItem> postalAddresses = new ArrayList<>();
    private List<ImAddressItem> imAddresses = new ArrayList<>();
    private List<UrlAddressItem> urlAddresses = new ArrayList<>();
    private List<RelationshipItem> relationships = new ArrayList<>();

    public Contact(String contactId) {
      this.contactId = contactId;
    }

    // convert to react native object
    public WritableMap toMap(Set<String> fieldSet) throws ParseException {
      WritableMap contact = Arguments.createMap();
      contact.putString("id", contactId);
      contact.putString("name", !TextUtils.isEmpty(displayName) ? displayName : givenName + " " + familyName);
      if (!TextUtils.isEmpty(givenName)) {
        contact.putString("firstName", givenName);
      }
      if (!TextUtils.isEmpty(middleName)) {
        contact.putString("middleName", middleName);
      }
      if (!TextUtils.isEmpty(familyName)) {
        contact.putString("lastName", familyName);
      }
      if (!TextUtils.isEmpty(nickname)) {
        contact.putString("nickname", nickname);
      }
      if (!TextUtils.isEmpty(suffix)) {
        contact.putString("nameSuffix", suffix);
      }
      if (!TextUtils.isEmpty(phoneticFirstName)) {
        contact.putString("phoneticFirstName", phoneticFirstName);
      }
      if (!TextUtils.isEmpty(phoneticLastName)) {
        contact.putString("phoneticLastName", phoneticLastName);
      }
      if (!TextUtils.isEmpty(phoneticMiddleName)) {
        contact.putString("phoneticMiddleName", phoneticMiddleName);
      }
      if (!TextUtils.isEmpty(company)) {
        contact.putString("company", company);
      }
      if (!TextUtils.isEmpty(jobTitle)) {
        contact.putString("jobTitle", jobTitle);
      }
      if (!TextUtils.isEmpty(department)) {
        contact.putString("department", department);
      }
      contact.putBoolean("imageAvailable", this.hasPhoto);
      if (fieldSet.contains("thumbnail")) {
        WritableMap thumbnail = Arguments.createMap();
        thumbnail.putString("uri", this.hasPhoto ? photoUri.toString() : null);
        contact.putMap("thumbnail", thumbnail);
      }

      if (fieldSet.contains("note") && !TextUtils.isEmpty(note)) { // double if check with query
        contact.putString("note", note);
      }

      if (fieldSet.contains("phoneNumbers")) {
        WritableArray phoneNumbers = Arguments.createArray();
        for (Item item : phones) {
          WritableMap map = Arguments.createMap();
          map.putString("number", item.value);
          map.putString("label", item.label);
          map.putString("id", item.id);
          map.putBoolean("primary", item.primary);
          phoneNumbers.pushMap(map);
        }
        contact.putArray("phoneNumbers", phoneNumbers);
      }

      if (fieldSet.contains("emails")) {
        WritableArray emailAddresses = Arguments.createArray();
        for (Item item : emails) {
          WritableMap map = Arguments.createMap();
          map.putString("email", item.value);
          map.putString("label", item.label);
          map.putString("id", item.id);
          map.putBoolean("primary", item.primary);
          emailAddresses.pushMap(map);
        }
        contact.putArray("emails", emailAddresses);
      }

      if (fieldSet.contains("addresses")) {
        WritableArray postalAddresses = Arguments.createArray();
        for (PostalAddressItem item : this.postalAddresses) {
          postalAddresses.pushMap(item.map);
        }
        contact.putArray("addresses", postalAddresses);
      }

      if (fieldSet.contains("instantMessageAddresses")) {
        WritableArray imAddresses = Arguments.createArray();
        for (ImAddressItem item : this.imAddresses) {
          imAddresses.pushMap(item.map);
        }
        contact.putArray("instantMessageAddresses", imAddresses);
      }

      if (fieldSet.contains("urlAddresses")) {
        WritableArray urlAddresses = Arguments.createArray();
        for (UrlAddressItem item : this.urlAddresses) {
          urlAddresses.pushMap(item.map);
        }
        contact.putArray("urlAddresses", urlAddresses);
      }

      if (fieldSet.contains("relationships")) {
        WritableArray relationships = Arguments.createArray();
        for (RelationshipItem item : this.relationships) {
          relationships.pushMap(item.map);
        }
        contact.putArray("relationships", relationships);
      }

      boolean showBirthday = fieldSet.contains("birthday");
      boolean showDates = fieldSet.contains("dates");

      if (showDates || showBirthday) { // double if check with query with cursor
        boolean hasYear;
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat datePattern = new SimpleDateFormat ("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat noYearPattern = new SimpleDateFormat ("--MM-dd", Locale.getDefault());

        WritableArray datesArray = Arguments.createArray();
        for (Item item : dates) {
          WritableMap details = Arguments.createMap();
          String dateString = item.value;
          String label = item.label;

          hasYear = !dateString.startsWith("--");

          if (hasYear) {
            calendar.setTime(datePattern.parse(dateString));
          } else {
            calendar.setTime(noYearPattern.parse(dateString));
          }

          if (hasYear) {
            details.putInt("year", calendar.get(Calendar.YEAR));
          }
          details.putInt("month", calendar.get(Calendar.MONTH) + 1);
          details.putInt("day", calendar.get(Calendar.DAY_OF_MONTH));
          if (showBirthday && label.equals("birthday")) {
            contact.putMap("birthday", details);
          } else {
            details.putString("label", label);
            datesArray.pushMap(details);
          }
        }
        if (showDates && datesArray.size() > 0) {
          contact.putArray("dates", datesArray);
        }
      }

      return contact;
    }

    public static class Item {
      public String label;
      public String value;
      public boolean primary;
      public String id;

      public Item(String label, String value) {
        this.label = label;
        this.value = value;
      }

      public Item(String label, String value, int isPrimary, String id) {
        this.label = label;
        this.value = value;
        this.primary = isPrimary == 1;
        this.id = id;
      }
    }

    public static class PostalAddressItem {
      public final WritableMap map;

      public PostalAddressItem(Cursor cursor) {
        map = Arguments.createMap();

        map.putString("label", getLabel(cursor));
        putString(cursor, "formattedAddress", CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS);
        putString(cursor, "street", CommonDataKinds.StructuredPostal.STREET);
        putString(cursor, "poBox", CommonDataKinds.StructuredPostal.POBOX);
        putString(cursor, "neighborhood", CommonDataKinds.StructuredPostal.NEIGHBORHOOD);
        putString(cursor, "city", CommonDataKinds.StructuredPostal.CITY);
        putString(cursor, "region", CommonDataKinds.StructuredPostal.REGION);
        putString(cursor, "state", CommonDataKinds.StructuredPostal.REGION);
        putString(cursor, "postalCode", CommonDataKinds.StructuredPostal.POSTCODE);
        putString(cursor, "country", CommonDataKinds.StructuredPostal.COUNTRY);
        putString(cursor, "id", CommonDataKinds.StructuredPostal._ID);
      }

      private void putString(Cursor cursor, String key, String androidKey) {
        final String value = cursor.getString(cursor.getColumnIndex(androidKey));
        if (!TextUtils.isEmpty(value))
          map.putString(key, value);
      }

      static String getLabel(Cursor cursor) {
        switch (cursor.getInt(cursor.getColumnIndex(CommonDataKinds.StructuredPostal.TYPE))) {
          case CommonDataKinds.StructuredPostal.TYPE_HOME:
            return "home";
          case CommonDataKinds.StructuredPostal.TYPE_WORK:
            return "work";
          case CommonDataKinds.StructuredPostal.TYPE_OTHER:
            return "other";
          case CommonDataKinds.StructuredPostal.TYPE_CUSTOM:
            final String label = cursor.getString(cursor.getColumnIndex(CommonDataKinds.StructuredPostal.LABEL));
            return label != null ? label : "";
        }
        return "unknown";
      }
    }

    public static class ImAddressItem {
      public final WritableMap map;

      public ImAddressItem(Cursor cursor) {
        String username = cursor.getString(cursor.getColumnIndex(CommonDataKinds.Im.DATA));
        int type = cursor.getInt(cursor.getColumnIndex(CommonDataKinds.Im.TYPE));
        int protocol = cursor.getInt(cursor.getColumnIndex(CommonDataKinds.Im.PROTOCOL));
        long imId = cursor.getLong(cursor.getColumnIndex(CommonDataKinds.Im._ID));

        map = Arguments.createMap();

        String label, service;

        switch (type) {
          case CommonDataKinds.Im.TYPE_HOME:
            label = "home";
            break;
          case CommonDataKinds.Im.TYPE_OTHER:
            label = "other";
            break;
          case CommonDataKinds.Im.TYPE_WORK:
            label = "work";
            break;
          case CommonDataKinds.Im.TYPE_CUSTOM:
            label = "custom";
            break;
          default:
            label = "unknown";
        }

        switch (protocol) {
          case CommonDataKinds.Im.PROTOCOL_AIM:
            service = "aim";
            break;
          case CommonDataKinds.Im.PROTOCOL_GOOGLE_TALK:
            service = "googleTalk";
            break;
          case CommonDataKinds.Im.PROTOCOL_ICQ:
            service = "icq";
            break;
          case CommonDataKinds.Im.PROTOCOL_JABBER:
            service = "jabber";
            break;
          case CommonDataKinds.Im.PROTOCOL_MSN:
            service = "msn";
            break;
          case CommonDataKinds.Im.PROTOCOL_NETMEETING:
            service = "netmeeting";
            break;
          case CommonDataKinds.Im.PROTOCOL_QQ:
            service = "qq";
            break;
          case CommonDataKinds.Im.PROTOCOL_SKYPE:
            service = "skype";
            break;
          case CommonDataKinds.Im.PROTOCOL_YAHOO:
            service = "yahoo";
            break;
          case CommonDataKinds.Im.PROTOCOL_CUSTOM:
            service = "custom";
            break;
          default:
            service = "unknown";
        }

        map.putString("username", username);
        map.putString("label", label);
        map.putString("service", service);
        map.putString("id", String.valueOf(imId));
      }
    }

    public static class UrlAddressItem {
      public final WritableMap map;

      public UrlAddressItem(Cursor cursor) {
        map = Arguments.createMap();
        map.putString("url",cursor.getString(cursor.getColumnIndex(CommonDataKinds.Website.URL)));
        int type = cursor.getInt(cursor.getColumnIndex(CommonDataKinds.Website.TYPE));
        String label;

        switch (type) {
          case CommonDataKinds.Website.TYPE_HOME:
            label = "home";
            break;
          case CommonDataKinds.Website.TYPE_OTHER:
            label = "other";
            break;
          case CommonDataKinds.Website.TYPE_WORK:
            label = "work";
            break;
          case CommonDataKinds.Website.TYPE_BLOG:
            label = "blog";
            break;
          case CommonDataKinds.Website.TYPE_HOMEPAGE:
            label = "homepage";
            break;
          case CommonDataKinds.Website.TYPE_FTP:
            label = "ftp";
            break;
          case CommonDataKinds.Website.TYPE_PROFILE:
            label = "profile";
            break;
          case CommonDataKinds.Website.TYPE_CUSTOM:
            label = "custom";
            break;
          default:
            label = "unknown";
        }

        map.putString("label", label);
        map.putString("id", String.valueOf(cursor.getLong(cursor.getColumnIndex(CommonDataKinds.Website._ID))));
      }
    }

    public static class RelationshipItem {
      public final WritableMap map;

      public RelationshipItem(Cursor cursor) {
        map = Arguments.createMap();
        map.putString("name",cursor.getString(cursor.getColumnIndex(CommonDataKinds.Relation.NAME)));
        int type = cursor.getInt(cursor.getColumnIndex(CommonDataKinds.Relation.TYPE));
        String label;

        switch (type) {
          case CommonDataKinds.Relation.TYPE_ASSISTANT:
            label = "assistant";
            break;
          case CommonDataKinds.Relation.TYPE_BROTHER:
            label = "bother";
            break;
          case CommonDataKinds.Relation.TYPE_CHILD:
            label = "child";
            break;
          case CommonDataKinds.Relation.TYPE_DOMESTIC_PARTNER:
            label = "domesticPartner";
            break;
          case CommonDataKinds.Relation.TYPE_FATHER:
            label = "father";
            break;
          case CommonDataKinds.Relation.TYPE_FRIEND:
            label = "friend";
            break;
          case CommonDataKinds.Relation.TYPE_MANAGER:
            label = "manager";
            break;
          case CommonDataKinds.Relation.TYPE_MOTHER:
            label = "mother";
            break;
          case CommonDataKinds.Relation.TYPE_PARENT:
            label = "parent";
            break;
          case CommonDataKinds.Relation.TYPE_PARTNER:
            label = "partner";
            break;
          case CommonDataKinds.Relation.TYPE_REFERRED_BY:
            label = "referredBy";
            break;
          case CommonDataKinds.Relation.TYPE_RELATIVE:
            label = "relative";
            break;
          case CommonDataKinds.Relation.TYPE_SISTER:
            label = "sister";
            break;
          case CommonDataKinds.Relation.TYPE_SPOUSE:
            label = "spouse";
            break;
          case CommonDataKinds.Relation.TYPE_CUSTOM:
            label = "custom";
            break;
          default:
            label = "unknown";
        }

        map.putString("label", label);
        map.putString("id", String.valueOf(cursor.getLong(cursor.getColumnIndex(CommonDataKinds.Relation._ID))));
      }
    }
  }

  private Set<String> getFieldsSet(final ReadableArray fields) {
    Set<String> fieldStrings = new HashSet<>();
    for (int ii = 0; ii < fields.size(); ii++) {
      String field = fields.getString(ii);
      if (field != null) {
        fieldStrings.add(field);
      }
    }
    return fieldStrings;
  }
}
