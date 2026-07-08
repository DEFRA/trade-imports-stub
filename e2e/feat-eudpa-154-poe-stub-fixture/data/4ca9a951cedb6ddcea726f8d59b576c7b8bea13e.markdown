# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: e2e/pages/notification-view-submitted.spec.ts >> Notification view (SUBMITTED) >> lands on the notification view page
- Location: tests/e2e/pages/notification-view-submitted.spec.ts:36:3

# Error details

```
"beforeAll" hook timeout of 30000ms exceeded.
```

# Page snapshot

```yaml
- generic [active] [ref=e1]:
  - link "Skip to main content" [ref=e2] [cursor=pointer]:
    - /url: "#main-content"
  - banner [ref=e3]:
    - link "GOV.UK" [ref=e7] [cursor=pointer]:
      - /url: https://www.gov.uk/
      - img "GOV.UK" [ref=e8]
    - region "Service information" [ref=e21]:
      - generic [ref=e23]:
        - link "Animals" [ref=e25] [cursor=pointer]:
          - /url: /
        - navigation "Menu" [ref=e26]:
          - list [ref=e27]:
            - listitem [ref=e28]:
              - link "Home" [ref=e29] [cursor=pointer]:
                - /url: /
            - listitem [ref=e30]:
              - link "About" [ref=e31] [cursor=pointer]:
                - /url: /about
  - generic [ref=e32]:
    - generic [ref=e35]:
      - generic [ref=e36]:
        - img [ref=e37]
        - generic [ref=e40]: test.user11@defra.gov.uk
      - generic [ref=e41]: "|"
      - link "Sign out" [ref=e42] [cursor=pointer]:
        - /url: /signout
    - main [ref=e43]:
      - generic [ref=e45]:
        - link "Back" [ref=e46] [cursor=pointer]:
          - /url: /addresses
        - paragraph [ref=e47]:
          - generic [ref=e48]: GBN-AG-26-B3EEGY
        - heading "Transport Entry point and arrival at destination" [level=1] [ref=e50]:
          - generic [ref=e51]: Transport
          - text: Entry point and arrival at destination
        - generic [ref=e53]:
          - generic [ref=e54]:
            - generic [ref=e55]: What is the port of entry into Great Britain?
            - combobox "What is the port of entry into Great Britain?" [ref=e56]:
              - option "Select port of entry" [selected]
              - option "──────────" [disabled]
              - option "Aberdeen Airport (GB DYC)"
              - option "Aberdeen Harbour (GB ABD)"
              - option "Avonmouth Docks (GB AVO)"
              - option "Ayr Port (GB AYR)"
              - option "Barking Port (GB BKG)"
              - option "Birmingham International Airport (GB BHM)"
              - option "Cambridge Airport (GB CBG)"
              - option "Chatham Docks (GB CTM)"
              - option "Coventry Airport (GB CVT)"
              - option "Dartford, Thames Europort (GB DFD)"
              - option "Dundee Port (GB DUN)"
              - option "East Midlands Airport (GB EMA)"
              - option "Edinburgh Airport (GB EDI)"
              - option "Farnborough Airport (GB FBO)"
              - option "Felixstowe Port (GB FXT)"
              - option "Fishguard Port (GB FIS)"
              - option "Folkestone (GB FOL)"
              - option "Fraserburgh Harbour (GB FRB)"
              - option "Gatwick Airport (GB LGW)"
              - option "Glasgow Airport (GB GLW)"
              - option "Glasgow Prestwick Airport (GB PIK)"
              - option "Grangemouth Port (GB GRG)"
              - option "Greenock Port (GB GRK)"
              - option "Grimsby and Immingham Port (GB IMM)"
              - option "HES Humber Bulk Terminal (GB HMR)"
              - option "Harwich Port (GB HRW)"
              - option "Heathrow Airport (GB LHR)"
              - option "Heysham Port (GB HYM)"
              - option "Holyhead Port (GB HLY)"
              - option "Hull Port (GB HUL)"
              - option "Humberside Airport (GB HUY)"
              - option "Hythe Port (GB HTH)"
              - option "Invergordon Port (GB IVG)"
              - option "Killingholme Port (GB KGH)"
              - option "Leith Port (GB LEI)"
              - option "Liverpool Airport (GB LPL)"
              - option "Liverpool Docks (GB LIV)"
              - option "London Gateway Port (GB LGP)"
              - option "London Thamesport (GB THP)"
              - option "Luton Airport (GB LUT)"
              - option "Manchester Airport (GB MAN)"
              - option "Methil Port (GB MTH)"
              - option "Newhaven Port (GB NHV)"
              - option "Northampton Airport (GB NHP)"
              - option "Nottingham Port (GB NTG)"
              - option "Pembroke Port (GB PED)"
              - option "Pembroke Port (GB PEM)"
              - option "Perth Port (GB PER)"
              - option "Peterhead Port (GB PHD)"
              - option "Plymouth Port (GB PLY)"
              - option "Poole Port (GB POO)"
              - option "Port of Birkenhead (GB BRK)"
              - option "Port of Blyth (GB BLY)"
              - option "Port of Bristol (GB BRS)"
              - option "Port of Cairnryan (GB CYN)"
              - option "Port of Dagenham (GB DAG)"
              - option "Port of Dover (GB DVR)"
              - option "Port of Dover (Eastern) (GB DVRE)"
              - option "Port of Dover (Western) (GB DVRW)"
              - option "Port of Grimsby (GB GSY)"
              - option "Port of Isle of Grain (GB IOG)"
              - option "Port of Manchester (GB MNC)"
              - option "Port of Middlesbrough (GB MID)"
              - option "Port of Sheerness (GBSHS)"
              - option "Port of Sheerness (GB SHS)"
              - option "Port of Tyne (GB TYN)"
              - option "Portsmouth Port (GB PME)"
              - option "Purfleet Port (GB PFT)"
              - option "Royal Portbury Dock (GB PRU)"
              - option "Silloth Port (GB SIL)"
              - option "Southampton Port (GB SOU)"
              - option "Stansted Airport (GB STN)"
              - option "Stranraer Harbour (GB STR)"
              - option "TILBURY (GB TIL)"
              - option "Teesside International Airport (GB MME)"
              - option "Teestort (GB TEE)"
              - option "Tilbury 2 Ro-Ro Terminal (GB TILR)"
              - option "Tilbury Port (GB TILL)"
          - group "When will the consignment arrive at its final destination?" [ref=e58]:
            - generic [ref=e59]: When will the consignment arrive at its final destination?
            - generic [ref=e60]: For example, 27 3 2026
            - generic [ref=e61]:
              - generic [ref=e63]:
                - generic [ref=e64]: Day
                - textbox "Day" [ref=e65]
              - generic [ref=e67]:
                - generic [ref=e68]: Month
                - textbox "Month" [ref=e69]
              - generic [ref=e71]:
                - generic [ref=e72]: Year
                - textbox "Year" [ref=e73]
          - button "Save and continue" [ref=e75] [cursor=pointer]
  - contentinfo [ref=e76]:
    - generic [ref=e89]:
      - generic [ref=e90]:
        - heading "Support links" [level=2] [ref=e91]
        - list [ref=e92]:
          - listitem [ref=e93]:
            - link "Privacy" [ref=e94] [cursor=pointer]:
              - /url: https://www.gov.uk/help/privacy-notice
          - listitem [ref=e95]:
            - link "Cookies" [ref=e96] [cursor=pointer]:
              - /url: https://www.gov.uk/help/cookies
          - listitem [ref=e97]:
            - link "Accessibility statement" [ref=e98] [cursor=pointer]:
              - /url: https://www.gov.uk/help/accessibility-statement
        - img [ref=e99]
        - generic [ref=e101]:
          - text: All content is available under the
          - link "Open Government Licence v3.0" [ref=e102] [cursor=pointer]:
            - /url: https://www.nationalarchives.gov.uk/doc/open-government-licence/version/3/
          - text: ", except where otherwise stated"
      - link "© Crown copyright" [ref=e104] [cursor=pointer]:
        - /url: https://www.nationalarchives.gov.uk/information-management/re-using-public-sector-information/uk-government-licensing-framework/crown-copyright/
```

# Test source

```ts
  1   | import { test, expect } from '@fixtures';
  2   | import { createPageObjects } from '@page-objects';
  3   | import {
  4   |   Journey,
  5   |   JourneyContext,
  6   |   defaultJourneyOptions,
  7   |   EAR_TAG_PREFIX,
  8   |   CONSIGNOR_NAME,
  9   |   DESTINATION_NAME,
  10  |   CPH_NUMBER,
  11  |   TRANSPORTER_NAME,
  12  | } from '@flows/journey';
  13  | import { getRelativeDate, toDisplayDate } from '@utils/date-utils';
  14  | import { camelCaseToSentenceCase, camelCaseToTitleCase } from '@utils/string-utils';
  15  | import { countryCodes } from '@domain/constants/country-codes';
  16  | 
  17  | test.describe('Notification view (SUBMITTED)', () => {
  18  |   const defaults = defaultJourneyOptions;
  19  |   let referenceNumber: string;
  20  | 
> 21  |   test.beforeAll(async ({ browser }) => {
      |        ^ "beforeAll" hook timeout of 30000ms exceeded.
  22  |     const context = await browser.newContext();
  23  |     const page = await context.newPage();
  24  |     const pages = createPageObjects(page);
  25  |     const journeyContext: JourneyContext = {};
  26  |     const journey = new Journey(pages, journeyContext);
  27  |     await journey.submitNotification();
  28  |     referenceNumber = journeyContext.notificationId;
  29  |     await context.close();
  30  |   });
  31  | 
  32  |   test.beforeEach(async ({ notificationActions }) => {
  33  |     await notificationActions.toNotificationView(referenceNumber);
  34  |   });
  35  | 
  36  |   test('lands on the notification view page', async ({ pages }) => {
  37  |     await expect(pages.page).toHaveURL(new RegExp(pages.notificationView.expectedUrl(referenceNumber)));
  38  |     await expect(pages.notificationView.heading).toBeVisible();
  39  |     await expect(pages.notificationView.referenceNumberCaption).toContainText(referenceNumber);
  40  |   });
  41  | 
  42  |   test('displays date created', async ({ pages }) => {
  43  |     const expectedDateCreated = toDisplayDate(getRelativeDate());
  44  |     await expect(pages.notificationView.dateCreated).toHaveText(`Date created: ${expectedDateCreated}`);
  45  |   });
  46  | 
  47  |   test('does not show Change links for a SUBMITTED notification', async ({ pages }) => {
  48  |     await expect(pages.notificationView.changeLink('Where is this consignment coming from?')).not.toBeVisible();
  49  |     await expect(pages.notificationView.changeLink('Your commodities')).not.toBeVisible();
  50  |     await expect(pages.notificationView.changeLink('Addresses')).not.toBeVisible();
  51  |   });
  52  | 
  53  |   test('shows Copy as new button', async ({ pages }) => {
  54  |     await expect(pages.notificationView.btnCopyAsNew).toBeVisible();
  55  |   });
  56  | 
  57  |   test('does not show Confirm and submit button for a SUBMITTED notification', async ({ pages }) => {
  58  |     await expect(pages.notificationView.btnConfirmAndSubmit).not.toBeVisible();
  59  |   });
  60  | 
  61  |   test('displays all section headings', async ({ pages }) => {
  62  |     await expect(pages.notificationView.sectionHeading('Where is this consignment coming from?')).toBeVisible();
  63  |     await expect(pages.notificationView.sectionHeading('Your commodities')).toBeVisible();
  64  |     await expect(pages.notificationView.sectionHeading('Additional information details')).toBeVisible();
  65  |     await expect(pages.notificationView.sectionHeading('Reason for importing the animals')).toBeVisible();
  66  |     await expect(pages.notificationView.sectionHeading('Addresses')).toBeVisible();
  67  |     await expect(pages.notificationView.sectionHeading('County Parish Holding number (CPH)')).toBeVisible();
  68  |     await expect(pages.notificationView.sectionHeading('Transport details')).toBeVisible();
  69  |     await expect(pages.notificationView.sectionHeading('Accompanying documents')).toBeVisible();
  70  |   });
  71  | 
  72  |   test('shows origin details', async ({ pages }) => {
  73  |     const country = Object.entries(countryCodes.eu).find(([, code]) => code === defaults.countryCode)[0];
  74  |     await expect(pages.notificationView.summaryValue('Country of origin')).toHaveText(camelCaseToTitleCase(country));
  75  |   });
  76  | 
  77  |   test('shows commodity name', async ({ pages }) => {
  78  |     await expect(pages.notificationView.commodityName).toContainText(defaults.commodityCode);
  79  |   });
  80  | 
  81  |   test('shows species rows with ear tag', async ({ pages }) => {
  82  |     await expect(pages.notificationView.speciesRows).toHaveCount(defaults.species.length);
  83  |     await expect(pages.notificationView.speciesCell(0, 0)).toContainText(defaults.species[0]);
  84  |     await expect(pages.notificationView.speciesCell(0, 1)).toContainText(EAR_TAG_PREFIX);
  85  |     await expect(pages.notificationView.speciesCell(1, 0)).toContainText(defaults.species[1]);
  86  |     await expect(pages.notificationView.speciesCell(1, 1)).toContainText(EAR_TAG_PREFIX);
  87  |   });
  88  | 
  89  |   test('shows reason for import', async ({ pages }) => {
  90  |     const expectedImportReason = camelCaseToSentenceCase(defaults.importReason);
  91  |     await expect(pages.notificationView.summaryValue('Main reason for importing the animals')).toHaveText(expectedImportReason);
  92  |   });
  93  | 
  94  |   test('shows consignor in addresses section', async ({ pages }) => {
  95  |     await expect(pages.notificationView.summaryValue('Consignor')).toContainText(CONSIGNOR_NAME);
  96  |   });
  97  | 
  98  |   test('shows place of destination in addresses section', async ({ pages }) => {
  99  |     await expect(pages.notificationView.summaryValue('Place of destination')).toContainText(DESTINATION_NAME);
  100 |   });
  101 | 
  102 |   test('shows CPH number', async ({ pages }) => {
  103 |     await expect(pages.notificationView.summaryValue('County Parish Holding number (CPH)')).toHaveText(CPH_NUMBER);
  104 |   });
  105 | 
  106 |   test('shows transporter name', async ({ pages }) => {
  107 |     await expect(pages.notificationView.summaryValue('Transporter name')).toContainText(TRANSPORTER_NAME);
  108 |   });
  109 | 
  110 |   test('shows port of entry', async ({ pages }) => {
  111 |     await expect(pages.notificationView.summaryValue('Port of entry')).toContainText(defaults.pointOfEntry.code);
  112 |   });
  113 | 
  114 |   test('shows no accompanying documents', async ({ pages }) => {
  115 |     // TODO: Pending automation of accompanying documents page (upload doc).
  116 |     await expect(pages.notificationView.noDocumentsText).toBeVisible();
  117 |   });
  118 | });
  119 | 
```