package wizardPages;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;

import utilities.Constants;

/**
 * Page with all of the generic welcome information, disclaimers, and summaries.
 * @author port091
 * @author rodr144
 */

public class Page_Welcome  extends WizardPage implements AbstractWizardPage {

	private ScrolledComposite sc;
	private Composite container;
	private Composite rootContainer;

	private boolean isCurrentPage = false;

	protected Page_Welcome() {
		super("Welcome");
	} 

	@Override
	public void createControl (Composite parent) {
		rootContainer = new Composite(parent, SWT.NULL);
		rootContainer.setLayout(GridLayoutFactory.fillDefaults().create());

		sc = new ScrolledComposite(rootContainer, SWT.V_SCROLL | SWT.H_SCROLL);
		sc.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, 480).create());
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);
		sc.addListener(SWT.Activate, new Listener() {
	        public void handleEvent(Event e) {
	            sc.setFocus();
	        }
	    });
		sc.addListener(SWT.MouseWheel, new Listener() {
	        public void handleEvent(Event event) {
	            int wheelCount = event.count;
	            wheelCount = (int) Math.ceil(wheelCount / 3.0f);
	            while (wheelCount < 0) {
	                sc.getVerticalBar().setIncrement(4);
	                wheelCount++;
	            }

	            while (wheelCount > 0) {
	                sc.getVerticalBar().setIncrement(-4);
	                wheelCount--;
	            }
	            sc.redraw();
	        }
	    });

		container = new Composite(sc, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.horizontalSpacing = 12;
		layout.verticalSpacing = 2;
		layout.numColumns = 4;
		layout.makeColumnsEqualWidth = false;
		container.setLayout(layout);
		
		sc.setContent(container);
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		setControl(rootContainer);
		setPageComplete(true);
	}

	@Override
	public void loadPage(boolean reset) {
		
		isCurrentPage = true;
		DREAMWizard.errorMessage.setText("");
		DREAMWizard.convertDataButton.setEnabled(false);
		DREAMWizard.visLauncher.setEnabled(false);
		DREAMWizard.nextButton.setVisible(true);
		
		Label infoLabel1 = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP);
		infoLabel1.setText("Welcome");
		infoLabel1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, ((GridLayout)container.getLayout()).numColumns, 2));
		infoLabel1.setFont(Constants.boldFont);
		
		GridData aboutInfoData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING);
		aboutInfoData.horizontalSpan = 2;
		aboutInfoData.widthHint = 440;
		Label aboutInfo = new Label(container,SWT.WRAP);
		aboutInfo.setText("The DREAM tool is an optimization software that generates monitoring campaigns which detect carbon dioxide "
				+ "(CO2) leaks while minimizing cost, time to detection, and aquifer degradation. DREAM reads ensembles of CO2 leak "
				+ "scenarios and determines optimal monitoring campaigns to deploy based on user-identified constraints. Multiple "
				+ "campaigns are identified and compared for their opportunity trade-offs to make informed monitoring decisions. "
				+ "\n\nDREAM  was developed as part of the National Risk Assessment Partnership. For more information: https://edx.netl.doe.gov/nrap/");
		// If windows sets text size to 150% or more, text doesn't fit
		if (Display.getCurrent().getDPI().x > 96) // 96dpi is 100%, 120dpi is 125%, 144dpi is 150%
			Constants.normalFontSmall = new Font(Display.getCurrent(), new FontData("Helvitica", 8, SWT.NORMAL));
		aboutInfo.setFont(Constants.normalFontSmall);
		aboutInfo.setLayoutData(aboutInfoData);
		
		GridData dreamImageData = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_BEGINNING);
		Image dreamImage = new Image(container.getDisplay(), getClass().getResourceAsStream("/DreamConcept.jpg"));
		dreamImageData.horizontalSpan = 2;
		dreamImageData.heightHint = 262;
		dreamImageData.minimumHeight = 262;
		CLabel dreamImageLabel = new CLabel(container, SWT.BORDER_SOLID);
		dreamImageLabel.setImage(dreamImage);
		dreamImageLabel.setLayoutData(dreamImageData);

		// NRAP logo at the bottom
		GridData nrapImageData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING);
		nrapImageData.horizontalSpan = 2;
		nrapImageData.verticalSpan = 8;
		nrapImageData.heightHint = 110;
		nrapImageData.minimumHeight = 110;
		Image nrapLogo = new Image(container.getDisplay(), getClass().getResourceAsStream("/NRAP.png"));
		CLabel nrapLogoLabel = new CLabel(container, SWT.BORDER_SOLID);
		nrapLogoLabel.setImage(nrapLogo);
		nrapLogoLabel.setLayoutData(nrapImageData);
		
		
		new Label(container, SWT.BEGINNING).setText("        Primary contact: Alex Hanna");
		new Label(container, SWT.BEGINNING);
		
		new Label(container, SWT.BEGINNING).setText("        Email: alexander.hanna@pnnl.gov");
		Link acknowledgements = new Link(container, SWT.BEGINNING);
		acknowledgements.setText("    <A>Acknowledgements</A>");
		acknowledgements.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				MessageBox messageBox = new MessageBox(Page_Welcome.this.getShell(), SWT.OK );
				messageBox.setMessage("Acknowledgements");
				messageBox.setMessage("This work was completed as part of the National Risk Assessment Partnership (NRAP) project. Support for this project came from the U.S. Department of Energy's (DOE) Office of Fossil Energy's Crosscutting Research program. "
						+ "The authors wish to acknowledge Traci Rodosta (Carbon Storage Technology Manager), Kanwal Mahajan (Carbon Storage Division Director), M. Kylee Rice (Carbon Storage Division Project Manager), Mark Ackiewicz (Division of CCS Research Program Manager),"
						+ "Robert Romanosky (NETL Crosscutting Research, Office of Strategic Planning), and Regis Conrad (DOE Office of Fossil Energy) for programmatic guidance, direction, and support. "
						+ "The authors wish to thank Catherine Yonkofski, Art Sadovsky, Jason Gastelum, Ellen Porter, Luke Rodriguez for their early development work on the DREAM tool.");
				messageBox.setText("Acknowledgements");
				messageBox.open();
			}
		});
		
		new Label(container, SWT.BEGINNING).setText("        Developers: Jonathan Whiting, Brian Huang");
		Link userManual = new Link(container, SWT.BEGINNING);
		userManual.setText("    <A>User manual</A>");
		userManual.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				// We essentially create a local copy of the pdf so that it works when packaged in a JAR
				try {
			        Path tempOutput = Files.createTempFile("user_manual", ".pdf");
			        tempOutput.toFile().deleteOnExit();
			        InputStream is = getClass().getResourceAsStream("/user_manual.pdf");
			        Files.copy(is, tempOutput, StandardCopyOption.REPLACE_EXISTING);
			        Desktop.getDesktop().open(tempOutput.toFile());
			        is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		
		new Label(container, SWT.BEGINNING).setText("        Testers: Delphine Appriou, Shyla Kupis");
		Link references = new Link(container, SWT.BEGINNING);
		references.setText("    <A>References</A>");
		references.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				MessageBox messageBox = new MessageBox(Page_Welcome.this.getShell(), SWT.OK );
				messageBox.setMessage("Yonkofski, C.M., Gastelum, J.A., Porter, E.A., Rodriguez, L.R., Bacon, D.H. and Brown, C.F., 2016. An optimization approach to design monitoring schemes for CO2 leakage detection. International Journal of Greenhouse Gas Control, 47, pp.233-239.");
				messageBox.setText("References");
				messageBox.open();
			}
		});
		
		new Label(container, SWT.BEGINNING).setText("        Version: 3.01 (2022)");
		
		// Lab logo at the bottom
		GridData imageData = new GridData(SWT.CENTER | SWT.BEGINNING);
		imageData.horizontalSpan = 4;
		imageData.heightHint = 100;
		imageData.minimumHeight = 86;
		Image labLogos = new Image(container.getDisplay(), getClass().getResourceAsStream("/DOE-LABS_S.png"));
		CLabel labLogosLabel = new CLabel(container, SWT.BORDER_SOLID);
		labLogosLabel.setImage(labLogos);
		labLogosLabel.setLayoutData(imageData);
		
		Page_Welcome.this.getShell().forceFocus(); //prevents the highlight of the acknowledgement link on load
		
		// Auto-test for an HDF5 and IAM example, simulates clicking through GUI
		if(reset && (Constants.skipToEnd || Constants.autoTest))
			DREAMWizard.nextButton.notifyListeners(SWT.Selection, new Event());
	}
	
	@Override
	public void completePage() throws Exception {
		isCurrentPage = false;
	}
	
	@Override
	public boolean isPageCurrent() {
		return isCurrentPage;
	}
	
	@Override
	public void setPageCurrent(boolean current) {
		isCurrentPage = current;
	}
}