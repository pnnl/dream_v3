package dialog;

import java.util.ArrayList;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import objects.NodeStructure;

public class InitUnitsDialog extends TitleAreaDialog {
	
	private NodeStructure node;
	
	private Composite container;
	private ScrolledComposite sc;
	private Label unitText;
	private Combo unitDropDown;
	private Label zText;
	private Combo zOrientationDropDown;
	private Label timeText;
	private Combo timeDropDown;
	private Label porosity;
	private Text porosityText;
	
	private ArrayList<ParameterUnit> units;
	private boolean missingParameterUnits;
	

	public InitUnitsDialog(Shell parentShell, NodeStructure node, boolean missingParameterUnits) {
		super(parentShell);
		this.node = node;
		this.missingParameterUnits = missingParameterUnits;
	}
	
	@Override
	public void create() {
		super.create();
		setTitle("Set Units");
		String message = "Set attributes such as units, Z orientation, and porosity that are missing from the input files.";
		setMessage(message, IMessageProvider.INFORMATION);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		
		sc = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.FILL);
		sc.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, 200).create());
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);
		
		container = new Composite(sc, SWT.NONE);
		buildThings();
		return area;
	}
	
	private void buildThings() {
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		container.setLayout(new GridLayout(2, false));
		
		// XYZ Unit
		if (node.getUnit("x").equals("")) {
			unitText = new Label(container, SWT.NONE | SWT.RIGHT);
			unitText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
			unitText.setText("XYZ Units:");
			unitDropDown = new Combo(container, SWT.DROP_DOWN | SWT.BORDER | SWT.LEFT);
			unitDropDown.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
			unitDropDown.add("m");
			unitDropDown.add("ft");
			unitDropDown.select(0);
		}
		
		// Z Orientation
		if (node.getPositive().equals("")) {
			zText = new Label(container, SWT.NONE | SWT.RIGHT);
			zText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
			zText.setText("Z-Axis Positive Direction: ");
			zOrientationDropDown = new Combo(container, SWT.DROP_DOWN | SWT.BORDER | SWT.LEFT);
			zOrientationDropDown.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
			zOrientationDropDown.add("up");
			zOrientationDropDown.add("down");
			zOrientationDropDown.select(0);
			// Negative numbers usually imply down as the direction, change default
			if(node.getZ().get(node.getZ().size()-1) < 0 || node.getZ().get(0) < 0)
				zOrientationDropDown.select(1);
		}
		
		// Time Unit
		if (node.getUnit("times").equals("")) {
			timeText = new Label(container, SWT.NONE | SWT.RIGHT);
			timeText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
			timeText.setText("Time Units: ");
			timeDropDown = new Combo(container, SWT.DROP_DOWN | SWT.BORDER | SWT.LEFT);
			timeDropDown.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
			timeDropDown.add("years");
			timeDropDown.add("months");
			timeDropDown.add("days");
			timeDropDown.select(0);
		}
		
		// Porosity
		if (!node.porosityIsSet()) {
			porosity = new Label(container, SWT.NONE | SWT.RIGHT);
			porosity.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
			porosity.setText("Set Porosity: ");
			porosityText = new Text(container, SWT.BORDER | SWT.LEFT);
			porosityText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
			porosityText.setText("0.1");
		}
		
		// Parameter Units
		if(missingParameterUnits) {
			units = new ArrayList<ParameterUnit>();
			for (String parameter : node.getParameters()) {
				units.add(new ParameterUnit(parameter));
			}
			for (ParameterUnit parameterUnit : units) {
				parameterUnit.createField();
			}
		}
		
		container.layout();
		sc.setContent(container);
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, OK, "OK", true);
	}
	
	protected Button createButton(Composite parent, int id, String label, boolean defaultButton){
		return super.createButton(parent, id, label, defaultButton);
	}
	
	@Override
	protected void buttonPressed(int id){
		if(id == OK){
			//Set the units if it doesn't exist.
			if (node.getUnit("x").equals("")) {
				String distance = unitDropDown.getText();
				node.addUnit("x", distance);
			}
			if (node.getPositive().equals("")) {
				String ZOrient = zOrientationDropDown.getText();
				node.addPositive(ZOrient);
			}
			if (node.getUnit("times").equals("")) {
				String time = timeDropDown.getText();
				node.addUnit("times", time);
			}
			if (!node.porosityIsSet()) {
				node.setPorosity(Float.valueOf(porosityText.getText()));
			}
			if (missingParameterUnits) {
				for(ParameterUnit parameterUnit : units) {
					node.addUnit(parameterUnit.getParameter(), parameterUnit.getUnit());
				}
			}
			super.okPressed();
		}
	}
	
	public class ParameterUnit {
		private String parameter;
		private String unit;
		
		public ParameterUnit(String parameter) {
			this.parameter = parameter;
			unit = "";
			// In case there is already a unit
			if (node.getUnit(parameter)!="")
				unit = node.getUnit(parameter);
			// Can add some smart text matching here
			if (parameter.toLowerCase().contains("pressure")) {
				unit = "Pa";
			} else if (parameter.toLowerCase().contains("temperature")) {
				unit = "degC";
			}
		}
		
		public void createField() {
			Label parameterLabel = new Label(container, SWT.NONE | SWT.RIGHT);
			parameterLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
			parameterLabel.setText(parameter + " units: ");
			Text parameterText = new Text(container, SWT.BORDER | SWT.LEFT);
			parameterText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
			parameterText.setText(unit);
			parameterText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					unit = ((Text)e.getSource()).getText();
				}
			});
		}
		public String getParameter() {
			return parameter;
		}
		public String getUnit() {
			return unit;
		}
	}
}
