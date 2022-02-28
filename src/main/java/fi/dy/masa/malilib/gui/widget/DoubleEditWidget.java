package fi.dy.masa.malilib.gui.widget;

import java.util.function.DoubleConsumer;
import net.minecraft.util.math.MathHelper;
import fi.dy.masa.malilib.gui.BaseScreen;
import fi.dy.masa.malilib.gui.callback.DoubleSliderCallback;
import fi.dy.masa.malilib.util.data.RangedDoubleStorage;

public class DoubleEditWidget extends BaseNumberEditWidget implements RangedDoubleStorage
{
    protected final DoubleConsumer consumer;
    protected final double minValue;
    protected final double maxValue;
    protected double value;

    public DoubleEditWidget(int width, int height, double originalValue,
                            double minValue, double maxValue, DoubleConsumer consumer)
    {
        super(width, height);

        this.consumer = consumer;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.setDoubleValue(originalValue);

        this.textFieldWidget.setText(String.valueOf(originalValue));
        this.textFieldWidget.setTextValidator(new DoubleTextFieldWidget.DoubleValidator(minValue, maxValue));
    }

    @Override
    protected SliderWidget createSliderWidget()
    {
        return new SliderWidget(-1, this.getHeight(), new DoubleSliderCallback(this, this::updateTextField));
    }

    @Override
    protected boolean onValueAdjustButtonClick(int mouseButton)
    {
        double amount = mouseButton == 1 ? -0.25 : 0.25;
        if (BaseScreen.isShiftDown()) { amount *= 4.0; }
        if (BaseScreen.isAltDown()) { amount *= 8.0; }

        this.setDoubleValue(this.value + amount);
        this.consumer.accept(this.value);

        return true;
    }

    protected void updateTextField()
    {
        this.textFieldWidget.setText(String.valueOf(this.value));
    }

    @Override
    protected void setValueFromTextField(String str)
    {
        try
        {
            this.clampAndSetValue(Double.parseDouble(str));
            this.consumer.accept(this.value);
        }
        catch (NumberFormatException ignore) {}
    }

    protected void clampAndSetValue(double newValue)
    {
        this.value = MathHelper.clamp(newValue, this.minValue, this.maxValue);
    }

    @Override
    public boolean setDoubleValue(double newValue)
    {
        this.clampAndSetValue(newValue);
        this.updateTextField();
        return true;
    }

    @Override
    public double getDoubleValue()
    {
        return this.value;
    }

    @Override
    public double getMinDoubleValue()
    {
        return this.minValue;
    }

    @Override
    public double getMaxDoubleValue()
    {
        return this.maxValue;
    }
}
